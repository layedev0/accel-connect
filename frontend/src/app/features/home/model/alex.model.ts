export interface Message {
  id: number;
  type: 'bot' | 'user';
  content: string;
  time: string;
}
export interface ChatResponse {
  response: string;
}

export interface ChatRequest {
  message: string;
}
