/**
 * Base HTTP Client for SecureFileShare API
 * WHAT: Wraps native fetch, handles JWT attachment, JSON serialization, and error normalization.
 * WHY: Centralizes authentication header injection and unified error handling across all microservices.
 */

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface ApiErrorResponse {
  status: number;
  error: string;
  message: string;
  path?: string;
  timestamp?: string;
}

export class ApiException extends Error {
  constructor(
    public status: number,
    public error: string,
    message: string,
    public path?: string
  ) {
    super(message);
    this.name = 'ApiException';
  }
}

// In-memory or sessionStorage token getter/setter
const TOKEN_KEY = 'sfs_jwt_token';
const USER_KEY = 'sfs_user_info';

export function getAuthToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function setAuthToken(token: string | null): void {
  if (token) {
    sessionStorage.setItem(TOKEN_KEY, token);
  } else {
    sessionStorage.removeItem(TOKEN_KEY);
  }
}

export interface UserSession {
  id: string;
  username: string;
  email: string;
  role: string;
}

export function getCurrentUser(): UserSession | null {
  const data = sessionStorage.getItem(USER_KEY);
  return data ? JSON.parse(data) : null;
}

export function setCurrentUser(user: UserSession | null): void {
  if (user) {
    sessionStorage.setItem(USER_KEY, JSON.stringify(user));
  } else {
    sessionStorage.removeItem(USER_KEY);
  }
}

export async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const headers = new Headers(options.headers || {});

  // Add JSON Content-Type if payload is not FormData
  if (!(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  // Attach Bearer token if available
  const token = getAuthToken();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(endpoint, {
    ...options,
    headers,
  });

  // Handle No-Content
  if (response.status === 204) {
    return {} as T;
  }

  // If response is not JSON (e.g. streaming binary or text)
  const contentType = response.headers.get('content-type') || '';
  if (!contentType.includes('application/json')) {
    if (!response.ok) {
      const text = await response.text();
      throw new ApiException(response.status, 'HttpError', text || response.statusText);
    }
    return response as unknown as T;
  }

  const json = await response.json();

  if (!response.ok) {
    const errorMsg = json.message || json.error || 'Request failed';
    throw new ApiException(response.status, json.error || 'Error', errorMsg, json.path);
  }

  // Return data unwrapped if wrapped in standard ApiResponse envelope
  if (json && typeof json === 'object' && 'data' in json && 'success' in json) {
    return json.data as T;
  }

  return json as T;
}
