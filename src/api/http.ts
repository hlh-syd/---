import axios from 'axios';

export const http = axios.create({
  baseURL: '/api',
  timeout: 8000,
});

http.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(error),
);
