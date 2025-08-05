import React, { useState, useRef } from 'react';
import { useSendMessage } from '../api/useSendMessage';
import { messageValidation } from '../utils/validation';
import { messageFormatter } from '../utils/messageFormatter';

interface SendMessageFormProps {
  roomId: string;
  placeholder?: string;
  maxLength?: number;
  disabled?: boolean;
  onSend?: (message: string) => void;
  className?: string;
}

export const SendMessageForm: React.FC<SendMessageFormProps> = ({
  roomId,
  placeholder = '메시지를 입력하세요...',
  maxLength = 500,
  disabled = false,
  onSend,
  className = '',
}) => {
  const [message, setMessage] = useState('');
  const [errors, setErrors] = useState<string[]>([]);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  
  const sendMessage = useSendMessage({
    onSuccess: () => {
      setMessage('');
      setErrors([]);
      onSend?.(message);
    },
    onError: (error) => {
      setErrors([error.message]);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (disabled || sendMessage.isPending) return;

    // 유효성 검증
    const validation = messageValidation.validateMessage(message);
    if (!validation.isValid) {
      setErrors(validation.errors);
      return;
    }

    // 특수 명령어 처리
    const command = messageFormatter.parseCommand(message);
    if (command.isCommand) {
      // 명령어 처리 로직 (추후 확장)
      console.log('Command:', command);
      return;
    }

    // 메시지 전송
    sendMessage.mutate({
      content: messageFormatter.processEmojis(message),
      roomId,
      messageType: 'CHAT',
    });
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    // Ctrl/Cmd + Enter로 전송
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault();
      handleSubmit(e as any);
      return;
    }

    // Enter만으로 전송 (Shift + Enter는 줄바꿈)
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e as any);
      return;
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const value = e.target.value;
    if (value.length <= maxLength) {
      setMessage(value);
      setErrors([]); // 입력 시 에러 클리어
    }
  };

  return (
    <form onSubmit={handleSubmit} className={`send-message-form ${className}`}>
      <div className="message-input-container">
        <textarea
          ref={textareaRef}
          value={message}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={disabled || sendMessage.isPending}
          className={`message-textarea ${errors.length > 0 ? 'error' : ''}`}
          rows={1}
          style={{ 
            resize: 'none',
            minHeight: '40px',
            maxHeight: '120px',
          }}
        />
        
        <div className="message-actions">
          <span className="character-count">
            {message.length}/{maxLength}
          </span>
          
          <button
            type="submit"
            disabled={disabled || sendMessage.isPending || message.trim().length === 0}
            className="send-button"
          >
            {sendMessage.isPending ? '전송중...' : '전송'}
          </button>
        </div>
      </div>

      {errors.length > 0 && (
        <div className="error-messages">
          {errors.map((error, index) => (
            <span key={index} className="error-message">
              {error}
            </span>
          ))}
        </div>
      )}
    </form>
  );
};