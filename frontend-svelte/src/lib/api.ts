// Thin client for the Spring Boot backend (separate app, see
// backend/src/main/java/com/be/web/controller/AuthController.java).
// Mirrors the contract the old static frontend used against the same API.

export const API_BASE_URL = (() => {
	if (typeof window === 'undefined') return 'http://localhost:8080/api/v1';
	const host = window.location.hostname;
	return host === 'localhost' || host === '127.0.0.1'
		? 'http://localhost:8080/api/v1'
		: 'https://api.tlab29.com/api/v1';
})();

export interface LoginResponse {
	token: string;
	tokenType: string;
	expiresIn: number;
	id: number;
	email: string;
	firstName: string;
	lastName: string;
	role: string;
}

export class ApiError extends Error {
	status: number;
	constructor(message: string, status: number) {
		super(message);
		this.status = status;
	}
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
	const res = await fetch(`${API_BASE_URL}${path}`, {
		...options,
		headers: { 'Content-Type': 'application/json', ...(options.headers ?? {}) }
	});
	const text = await res.text();
	const data = text ? JSON.parse(text) : null;
	if (!res.ok) {
		throw new ApiError(data?.message ?? data?.error ?? `HTTP ${res.status}`, res.status);
	}
	return data as T;
}

export function login(email: string, password: string) {
	return request<LoginResponse>('/auth/login', {
		method: 'POST',
		body: JSON.stringify({ email, password })
	});
}

export function register(input: {
	firstName: string;
	lastName: string;
	email: string;
	password: string;
}) {
	return request<LoginResponse>('/auth/register', {
		method: 'POST',
		body: JSON.stringify(input)
	});
}

export function persistSession(data: LoginResponse) {
	if (typeof window === 'undefined') return;
	localStorage.setItem('authToken', data.token);
	localStorage.setItem('tokenExpiry', String(Date.now() + data.expiresIn * 1000));
	localStorage.setItem('userData', JSON.stringify({ id: data.id, email: data.email, role: data.role }));
}
