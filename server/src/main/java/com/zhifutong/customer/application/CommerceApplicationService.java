package com.zhifutong.customer.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhifutong.customer.auth.AuthenticatedUser;
import com.zhifutong.customer.domain.OrderStatus;
import com.zhifutong.customer.domain.ProductSaleStatus;
import com.zhifutong.customer.domain.ShipmentStatus;
import com.zhifutong.customer.entity.CustomerOrder;
import com.zhifutong.customer.entity.ProductCatalog;
import com.zhifutong.customer.entity.ShipmentEvent;
import com.zhifutong.customer.exception.BusinessException;
import com.zhifutong.customer.mapper.CustomerOrderMapper;
import com.zhifutong.customer.mapper.ProductCatalogMapper;
import com.zhifutong.customer.mapper.ShipmentEventMapper;
import com.zhifutong.customer.vo.OrderResponse;
import com.zhifutong.customer.vo.PageResult;
import com.zhifutong.customer.vo.ProductResponse;
import com.zhifutong.customer.vo.ShipmentEventResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommerceApplicationService {
    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("(ORD[0-9A-Z]{8,})", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ProductCatalogMapper productMapper;
    private final CustomerOrderMapper orderMapper;
    private final ShipmentEventMapper shipmentMapper;

    public CommerceApplicationService(ProductCatalogMapper productMapper, CustomerOrderMapper orderMapper,
                                      ShipmentEventMapper shipmentMapper) {
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.shipmentMapper = shipmentMapper;
    }

    public PageResult<ProductResponse> listProducts(long page, long size, String keyword) {
        Page<ProductCatalog> result = productMapper.selectPage(Page.of(page, size),
                new LambdaQueryWrapper<ProductCatalog>()
                        .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                                .like(ProductCatalog::getProductName, keyword)
                                .or()
                                .like(ProductCatalog::getProductCode, keyword))
                        .orderByDesc(ProductCatalog::getUpdatedAt));
        return new PageResult<>(page, size, result.getTotal(), result.getRecords().stream().map(this::toProductResponse).toList());
    }

    public PageResult<OrderResponse> listMine(AuthenticatedUser user, long page, long size, OrderStatus status) {
        Page<CustomerOrder> result = orderMapper.selectPage(Page.of(page, size),
                new LambdaQueryWrapper<CustomerOrder>()
                        .eq(CustomerOrder::getUserId, user.userId())
                        .eq(status != null, CustomerOrder::getStatus, status)
                        .orderByDesc(CustomerOrder::getCreatedAt));
        return new PageResult<>(page, size, result.getTotal(), result.getRecords().stream().map(this::toOrderResponse).toList());
    }

    public PageResult<OrderResponse> listAllOrders(long page, long size, OrderStatus status, String keyword) {
        Page<CustomerOrder> result = orderMapper.selectPage(Page.of(page, size),
                new LambdaQueryWrapper<CustomerOrder>()
                        .eq(status != null, CustomerOrder::getStatus, status)
                        .like(keyword != null && !keyword.isBlank(), CustomerOrder::getOrderNo, keyword)
                        .orderByDesc(CustomerOrder::getCreatedAt));
        return new PageResult<>(page, size, result.getTotal(), result.getRecords().stream().map(this::toOrderResponse).toList());
    }

    public OrderResponse getMine(AuthenticatedUser user, Long id) {
        CustomerOrder order = requireOrder(id);
        if (!order.getUserId().equals(user.userId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "不能查看其他用户的订单");
        }
        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus status, String carrier, String trackingNo, String location, String eventNote) {
        CustomerOrder order = requireOrder(id);
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(status);
        order.setUpdatedAt(now);
        if ((status == OrderStatus.SHIPPED || status == OrderStatus.IN_TRANSIT) && order.getShippedAt() == null) {
            order.setShippedAt(now);
        }
        if (status == OrderStatus.SIGNED && order.getSignedAt() == null) {
            order.setSignedAt(now);
        }
        orderMapper.updateById(order);
        ShipmentEvent event = new ShipmentEvent();
        event.setOrderId(order.getId());
        event.setCarrier(carrier);
        event.setTrackingNo(trackingNo);
        event.setStatus(toShipmentStatus(status));
        event.setLocation(location);
        event.setEventNote((eventNote == null || eventNote.isBlank()) ? defaultShipmentNote(status) : eventNote);
        event.setEventTime(now);
        event.setCreatedAt(now);
        shipmentMapper.insert(event);
        return toOrderResponse(order);
    }

    public Optional<String> answerBusinessQuestion(AuthenticatedUser user, String question) {
        if (question == null || question.isBlank()) {
            return Optional.empty();
        }
        String clean = question.trim();
        Optional<CustomerOrder> matchedOrder = findOrderByNo(user, clean);
        if (matchedOrder.isEmpty() && looksLikeOrderQuestion(clean)) {
            matchedOrder = latestOrder(user);
        }
        if (matchedOrder.isPresent()) {
            return Optional.of(answerForOrder(matchedOrder.get()));
        }
        if (looksLikeProductQuestion(clean)) {
            ProductCatalog product = findMentionedProduct(clean);
            if (product != null) {
                return Optional.of(answerForProduct(product));
            }
        }
        return Optional.empty();
    }

    private Optional<CustomerOrder> findOrderByNo(AuthenticatedUser user, String question) {
        Matcher matcher = ORDER_NO_PATTERN.matcher(question.toUpperCase());
        if (!matcher.find()) {
            return Optional.empty();
        }
        CustomerOrder order = orderMapper.selectOne(new LambdaQueryWrapper<CustomerOrder>()
                .eq(CustomerOrder::getOrderNo, matcher.group(1))
                .last("LIMIT 1"));
        if (order == null || !order.getUserId().equals(user.userId())) {
            return Optional.empty();
        }
        return Optional.of(order);
    }

    private Optional<CustomerOrder> latestOrder(AuthenticatedUser user) {
        CustomerOrder order = orderMapper.selectOne(new LambdaQueryWrapper<CustomerOrder>()
                .eq(CustomerOrder::getUserId, user.userId())
                .orderByDesc(CustomerOrder::getCreatedAt)
                .last("LIMIT 1"));
        return Optional.ofNullable(order);
    }

    private boolean looksLikeOrderQuestion(String question) {
        return containsAny(question, "订单", "发货", "物流", "快递", "到哪", "签收", "配送", "什么时候到", "什么时候发");
    }

    private boolean looksLikeProductQuestion(String question) {
        return containsAny(question, "商品", "库存", "价格", "发货", "售后", "退货", "拆封", "质量");
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private ProductCatalog findMentionedProduct(String question) {
        List<ProductCatalog> products = productMapper.selectList(new LambdaQueryWrapper<ProductCatalog>()
                .orderByAsc(ProductCatalog::getId));
        for (ProductCatalog product : products) {
            if (question.contains(product.getProductName()) || question.toUpperCase().contains(product.getProductCode().toUpperCase())) {
                return product;
            }
        }
        return null;
    }

    private String answerForOrder(CustomerOrder order) {
        ProductCatalog product = productMapper.selectById(order.getProductId());
        ShipmentEvent latest = latestShipmentEvent(order.getId());
        String productName = product == null ? "商品" : product.getProductName();
        if (order.getStatus() == OrderStatus.WAITING_SHIPMENT || order.getStatus() == OrderStatus.PAID) {
            return "我查到您的订单 " + order.getOrderNo() + " 是「" + productName + "」，当前还未发货。预计发货时间是 "
                    + format(order.getExpectedShipAt()) + "。如果超过这个时间还没有物流更新，可以直接转人工帮您催一下仓库。";
        }
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.IN_TRANSIT) {
            String latestText = latest == null ? "暂时没有新的物流节点" : latest.getEventNote() + "（" + format(latest.getEventTime()) + "）";
            return "我查到您的订单 " + order.getOrderNo() + " 已发货，物流单号是 "
                    + valueOrDefault(latest == null ? null : latest.getTrackingNo(), "暂未同步") + "。最新进展：" + latestText + "。";
        }
        if (order.getStatus() == OrderStatus.SIGNED) {
            return "我查到您的订单 " + order.getOrderNo() + " 已在 " + format(order.getSignedAt())
                    + " 签收。如果商品有破损、缺件或质量问题，可以继续描述情况，我帮您转到售后工单。";
        }
        if (order.getStatus() == OrderStatus.REFUNDING || order.getStatus() == OrderStatus.REFUNDED) {
            return "我查到您的订单 " + order.getOrderNo() + " 当前处于" + order.getStatus()
                    + "状态，退款/售后进度建议在工单里继续跟进，避免遗漏凭证。";
        }
        return "我查到您的订单 " + order.getOrderNo() + " 当前状态是 " + order.getStatus()
                + "，商品是「" + productName + "」。如果需要更具体处理，可以转人工继续核实。";
    }

    private String answerForProduct(ProductCatalog product) {
        String stock = product.getSaleStatus() == ProductSaleStatus.ON_SALE
                ? "当前有库存 " + product.getStockQuantity() + " 件"
                : "当前状态为 " + product.getSaleStatus();
        return "「" + product.getProductName() + "」" + stock + "，售价 " + product.getPrice()
                + " 元。发货规则：" + product.getDispatchRule() + "。售后说明：" + product.getAfterSaleRule();
    }

    private ShipmentEvent latestShipmentEvent(Long orderId) {
        return shipmentMapper.selectOne(new LambdaQueryWrapper<ShipmentEvent>()
                .eq(ShipmentEvent::getOrderId, orderId)
                .orderByDesc(ShipmentEvent::getEventTime)
                .last("LIMIT 1"));
    }

    private CustomerOrder requireOrder(Long id) {
        CustomerOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private OrderResponse toOrderResponse(CustomerOrder order) {
        ProductCatalog product = productMapper.selectById(order.getProductId());
        List<ShipmentEventResponse> events = shipmentMapper.selectList(new LambdaQueryWrapper<ShipmentEvent>()
                        .eq(ShipmentEvent::getOrderId, order.getId())
                        .orderByDesc(ShipmentEvent::getEventTime))
                .stream()
                .map(this::toShipmentResponse)
                .toList();
        return new OrderResponse(order.getId(), order.getOrderNo(), order.getUserId(), toProductResponse(product),
                order.getQuantity(), order.getAmount(), order.getStatus(), order.getPaidAt(), order.getExpectedShipAt(),
                order.getShippedAt(), order.getSignedAt(), order.getReceiverName(), order.getReceiverPhone(),
                order.getReceiverAddress(), order.getRemark(), events, order.getCreatedAt(), order.getUpdatedAt());
    }

    private ProductResponse toProductResponse(ProductCatalog product) {
        if (product == null) {
            return null;
        }
        return new ProductResponse(product.getId(), product.getProductCode(), product.getProductName(), product.getCategory(),
                product.getSaleStatus(), product.getPrice(), product.getStockQuantity(), product.getDispatchRule(),
                product.getAfterSaleRule(), product.getCreatedAt(), product.getUpdatedAt());
    }

    private ShipmentEventResponse toShipmentResponse(ShipmentEvent event) {
        return new ShipmentEventResponse(event.getId(), event.getCarrier(), event.getTrackingNo(), event.getStatus(),
                event.getLocation(), event.getEventNote(), event.getEventTime());
    }

    private ShipmentStatus toShipmentStatus(OrderStatus status) {
        return switch (status) {
            case SHIPPED -> ShipmentStatus.PICKED_UP;
            case IN_TRANSIT -> ShipmentStatus.IN_TRANSIT;
            case SIGNED -> ShipmentStatus.DELIVERED;
            default -> ShipmentStatus.CREATED;
        };
    }

    private String defaultShipmentNote(OrderStatus status) {
        return switch (status) {
            case SHIPPED -> "包裹已交给快递";
            case IN_TRANSIT -> "包裹运输中";
            case SIGNED -> "订单已签收";
            default -> "订单状态已更新";
        };
    }

    private String format(LocalDateTime value) {
        return value == null ? "暂未同步" : DATE_TIME.format(value);
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
