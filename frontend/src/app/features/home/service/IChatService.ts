import { Observable } from "rxjs";
import { ChatResponse } from "../model/alex.model";

export interface IChatService {
    sendMessage(message: string): Observable<ChatResponse>;
}