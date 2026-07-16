package com.zhifutong.customer.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhifutong.customer.client.EmbeddingClient;
import com.zhifutong.customer.client.QdrantVectorStore;
import com.zhifutong.customer.config.AppProperties;
import com.zhifutong.customer.domain.DocumentStatus;
import com.zhifutong.customer.entity.KbDocument;
import com.zhifutong.customer.exception.BusinessException;
import com.zhifutong.customer.mapper.KbDocumentMapper;
import com.zhifutong.customer.rag.KnowledgeChunk;
import com.zhifutong.customer.rag.TextChunker;
import com.zhifutong.customer.service.DocumentParser;
import com.zhifutong.customer.service.FileValidator;
import com.zhifutong.customer.vo.DocumentResponse;
import com.zhifutong.customer.vo.PageResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentApplicationService {
    private final KbDocumentMapper documentMapper;
    private final FileValidator fileValidator;
    private final DocumentParser documentParser;
    private final TextChunker textChunker;
    private final EmbeddingClient embeddingClient;
    private final QdrantVectorStore vectorStore;
    private final AppProperties properties;

    public DocumentApplicationService(KbDocumentMapper documentMapper, FileValidator fileValidator,
                                      DocumentParser documentParser, TextChunker textChunker,
                                      EmbeddingClient embeddingClient, QdrantVectorStore vectorStore,
                                      AppProperties properties) {
        this.documentMapper = documentMapper;
        this.fileValidator = fileValidator;
        this.documentParser = documentParser;
        this.textChunker = textChunker;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    @Transactional
    public DocumentResponse upload(MultipartFile file) {
        String extension = fileValidator.validate(file);
        String originalName = file.getOriginalFilename();
        if (documentMapper.selectCount(new LambdaQueryWrapper<KbDocument>().eq(KbDocument::getOriginalName, originalName)) > 0) {
            throw new BusinessException("同名文档已存在，请删除后重新上传");
        }
        Path storageRoot = Path.of(properties.getDocument().getStoragePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException ex) {
            throw new BusinessException("文档存储目录创建失败");
        }
        String storageName = UUID.randomUUID() + "." + extension;
        Path target = storageRoot.resolve(storageName).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new BusinessException("文件存储路径不合法");
        }
        try {
            file.transferTo(target);
        } catch (IOException ex) {
            throw new BusinessException("文件保存失败");
        }

        LocalDateTime now = LocalDateTime.now();
        KbDocument document = new KbDocument();
        document.setOriginalName(originalName);
        document.setStorageName(storageName);
        document.setStoragePath(target.toString());
        document.setFileType(extension);
        document.setFileSize(file.getSize());
        document.setStatus(DocumentStatus.PENDING);
        document.setChunkCount(0);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        documentMapper.insert(document);

        process(document.getId());
        return toResponse(documentMapper.selectById(document.getId()));
    }

    @Transactional
    public DocumentResponse retry(Long id) {
        KbDocument document = require(id);
        if (!document.getStatus().canRetry()) {
            throw new BusinessException("只有失败文档可以重新处理");
        }
        process(id);
        return toResponse(documentMapper.selectById(id));
    }

    public void process(Long id) {
        KbDocument document = require(id);
        try {
            updateStatus(document, DocumentStatus.PROCESSING, null, 0);
            String text = documentParser.parse(Path.of(document.getStoragePath()), document.getFileType());
            List<String> texts = textChunker.split(text, properties.getRag().getChunkSize(),
                    properties.getRag().getChunkOverlap(), properties.getRag().getMinChunkLength());
            if (texts.isEmpty()) {
                throw new BusinessException("文档切分后没有有效片段");
            }
            List<KnowledgeChunk> chunks = new ArrayList<>();
            List<float[]> vectors = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                String chunkText = texts.get(i);
                chunks.add(new KnowledgeChunk(document.getId(), document.getOriginalName(), i, chunkText, 1.0));
                vectors.add(embeddingClient.embed(chunkText));
            }
            vectorStore.deleteByDocumentId(document.getId());
            vectorStore.upsert(chunks, vectors);
            updateStatus(document, DocumentStatus.COMPLETED, null, chunks.size());
        } catch (Exception ex) {
            try {
                vectorStore.deleteByDocumentId(document.getId());
            } catch (Exception ignored) {
                // Best-effort cleanup; original failure is recorded below.
            }
            updateStatus(document, DocumentStatus.FAILED, truncate(ex.getMessage()), 0);
            if (ex instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException("文档处理失败: " + ex.getMessage());
        }
    }

    public PageResult<DocumentResponse> list(long page, long size, String keyword) {
        Page<KbDocument> result = documentMapper.selectPage(Page.of(page, size),
                new LambdaQueryWrapper<KbDocument>()
                        .like(keyword != null && !keyword.isBlank(), KbDocument::getOriginalName, keyword)
                        .orderByDesc(KbDocument::getCreatedAt));
        return new PageResult<>(page, size, result.getTotal(), result.getRecords().stream().map(this::toResponse).toList());
    }

    public DocumentResponse get(Long id) {
        return toResponse(require(id));
    }

    public Resource download(Long id) {
        KbDocument document = require(id);
        Path path = Path.of(document.getStoragePath()).normalize();
        if (!Files.exists(path)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "文档文件不存在");
        }
        return new FileSystemResource(path);
    }

    @Transactional
    public String delete(Long id) {
        KbDocument document = require(id);
        boolean qdrantDeleted = false;
        boolean fileDeleted = false;
        try {
            vectorStore.deleteByDocumentId(id);
            qdrantDeleted = true;
            Path path = Path.of(document.getStoragePath());
            fileDeleted = !Files.exists(path) || Files.deleteIfExists(path);
            int dbDeleted = documentMapper.deleteById(id);
            if (!qdrantDeleted || !fileDeleted || dbDeleted != 1) {
                throw new BusinessException("文档删除不完整，请检查 Qdrant、磁盘文件和数据库状态");
            }
            return "Qdrant片段、磁盘文件和数据库记录均已删除";
        } catch (IOException ex) {
            throw new BusinessException("磁盘文件删除失败: " + ex.getMessage());
        }
    }

    public KbDocument require(Long id) {
        KbDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "文档不存在");
        }
        return document;
    }

    public DocumentResponse toResponse(KbDocument document) {
        return new DocumentResponse(document.getId(), document.getOriginalName(), document.getFileType(),
                document.getFileSize(), document.getStatus(), document.getChunkCount(), document.getFailureReason(),
                document.getCreatedAt(), document.getUpdatedAt());
    }

    private void updateStatus(KbDocument document, DocumentStatus status, String reason, int chunkCount) {
        document.setStatus(status);
        document.setFailureReason(reason);
        document.setChunkCount(chunkCount);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
    }

    private String truncate(String text) {
        if (text == null) {
            return "未知错误";
        }
        return text.length() <= 500 ? text : text.substring(0, 500);
    }
}
