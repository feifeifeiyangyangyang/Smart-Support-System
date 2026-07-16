import axios from 'axios'

export const api = axios.create({
  baseURL: '/api/v1',
  timeout: 20000
})

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  timestamp: string
}

export async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await promise
  if (!response.data.success) {
    throw new Error(response.data.message)
  }
  return response.data.data
}
