import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Message } from '../model/alex.model';
import { ChatServiceImpl } from '../service/impl/chat.service';
import { HttpClientModule } from '@angular/common/http';

@Component({
  selector: 'app-alex-chat-component',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  providers: [ChatServiceImpl],
  templateUrl: './alex-chat-component.html',
  styleUrl: './alex-chat-component.css',
})
export class AlexChatComponent {
  isOpen = false;
  isMinimized = false;
  message = '';
  isTyping = false;

  messages: Message[] = [
    {
      id: 1,
      type: 'bot',
      content:
        "Bonjour ! Je suis Alex, votre assistant virtuel. Comment puis-je vous aider aujourd'hui ?",
      time: this.getCurrentTime(),
    },
  ];

  private typingSpeed = 30;
  private isCurrentlyTyping = false;

  constructor(private readonly chatService: ChatServiceImpl) {}

  toggleChat(): void {
    this.isOpen = !this.isOpen;
    this.isMinimized = false;
  }

  closeChat(): void {
    this.isOpen = false;
  }

  minimizeChat(): void {
    this.isMinimized = !this.isMinimized;
  }

  sendMessage(): void {
    if (!this.message.trim() || this.isCurrentlyTyping) return;

    const newMessage: Message = {
      id: this.messages.length + 1,
      type: 'user',
      content: this.message,
      time: this.getCurrentTime(),
    };

    this.messages.push(newMessage);
    const userMessage = this.message;
    this.message = '';
    this.scrollToBottom();

    this.isTyping = true;

    this.chatService.sendMessage(userMessage).subscribe({
      next: (response) => {
        this.isTyping = false;
        // Formater la réponse avant de l'afficher
        const formattedResponse = this.formatResponse(response.response);
        this.typeWriterEffectByWords(formattedResponse);
      },
      error: (error) => {
        this.isTyping = false;
        this.typeWriterEffect(`Désolé, une erreur s'est produite : ${error.message}`);
      },
    });
  }

  /**
   * Formate la réponse pour convertir les sauts de ligne et listes en HTML
   */
  private formatResponse(text: string): string {
    // Convertir les numéros suivis de points en liste HTML
    let formatted = text.replace(/(\d+)\.\s+([^\n]+)/g, '<li>$2</li>');

    // Envelopper les listes dans des balises <ol>
    if (formatted.includes('<li>')) {
      formatted = formatted.replace(/(<li>.*<\/li>)/gs, '<ol class="formatted-list">$1</ol>');
    }

    // Convertir les tirets en liste à puces
    formatted = formatted.replace(/^[-•]\s+(.+)$/gm, '<li>$1</li>');

    // Envelopper les listes à puces dans des balises <ul>
    formatted = formatted.replace(
      /(<li>(?!.*<\/ol>).*<\/li>)/gs,
      '<ul class="formatted-list">$1</ul>'
    );

    // Convertir les sauts de ligne doubles en paragraphes
    formatted = formatted.replace(/\n\n/g, '</p><p class="message-paragraph">');

    // Convertir les sauts de ligne simples en <br>
    formatted = formatted.replace(/\n/g, '<br>');

    // Envelopper dans un paragraphe si pas déjà fait
    if (!formatted.includes('<p>') && !formatted.includes('<ol>') && !formatted.includes('<ul>')) {
      formatted = `<p class="message-paragraph">${formatted}</p>`;
    } else if (formatted.includes('</p><p>')) {
      formatted = `<p class="message-paragraph">${formatted}</p>`;
    }

    return formatted;
  }

  /**
   * Effet de machine à écrire pour les messages du bot
   */
  private typeWriterEffect(fullText: string): void {
    this.isCurrentlyTyping = true;

    const botMessage: Message = {
      id: this.messages.length + 1,
      type: 'bot',
      content: '',
      time: this.getCurrentTime(),
    };

    this.messages.push(botMessage);
    const messageIndex = this.messages.length - 1;

    let charIndex = 0;

    const typeInterval = setInterval(() => {
      if (charIndex < fullText.length) {
        this.messages[messageIndex].content += fullText.charAt(charIndex);
        charIndex++;
        this.scrollToBottom();
      } else {
        clearInterval(typeInterval);
        this.isCurrentlyTyping = false;
      }
    }, this.typingSpeed);
  }

  /**
   * Variante avec vitesse variable (plus rapide pour les espaces)
   */
  private typeWriterEffectVariable(fullText: string): void {
    this.isCurrentlyTyping = true;

    const botMessage: Message = {
      id: this.messages.length + 1,
      type: 'bot',
      content: '',
      time: this.getCurrentTime(),
    };

    this.messages.push(botMessage);
    const messageIndex = this.messages.length - 1;

    let charIndex = 0;

    const typeNextChar = () => {
      if (charIndex < fullText.length) {
        const currentChar = fullText.charAt(charIndex);
        this.messages[messageIndex].content += currentChar;
        charIndex++;

        this.scrollToBottom();

        let delay = this.typingSpeed;
        if (currentChar === ' ') {
          delay = this.typingSpeed * 0.5;
        } else if (['.', '!', '?', ','].includes(currentChar)) {
          delay = this.typingSpeed * 2;
        }

        setTimeout(typeNextChar, delay);
      } else {
        this.isCurrentlyTyping = false;
      }
    };

    typeNextChar();
  }

  /**
   * Méthode alternative avec effet de typing par mots
   */
  private typeWriterEffectByWords(fullText: string): void {
    this.isCurrentlyTyping = true;

    const botMessage: Message = {
      id: this.messages.length + 1,
      type: 'bot',
      content: '',
      time: this.getCurrentTime(),
    };

    this.messages.push(botMessage);
    const messageIndex = this.messages.length - 1;

    const words = fullText.split(' ');
    let wordIndex = 0;

    const typeInterval = setInterval(() => {
      if (wordIndex < words.length) {
        if (wordIndex > 0) {
          this.messages[messageIndex].content += ' ';
        }
        this.messages[messageIndex].content += words[wordIndex];
        wordIndex++;

        this.scrollToBottom();
      } else {
        clearInterval(typeInterval);
        this.isCurrentlyTyping = false;
      }
    }, 100);
  }

  handleKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  private getCurrentTime(): string {
    return new Date().toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      const chatBody = document.querySelector('.chat-messages-container');
      if (chatBody) {
        chatBody.scrollTop = chatBody.scrollHeight;
      }
    }, 0);
  }
}
