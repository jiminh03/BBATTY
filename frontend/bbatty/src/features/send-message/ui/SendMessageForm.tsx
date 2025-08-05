import React, { useState, useRef } from 'react';
import { View, Alert } from 'react-native';
import { Input } from '../../../shared/ui/atoms/Input/Input';
import { Button } from '../../../shared/ui/atoms/button/button';
import { Label } from '../../../shared/ui/atoms/Label/Label';
import { useSendMessage } from '../api/useSendMessage';
import { messageValidation } from '../utils/validation';
import { messageFormatter } from '../utils/messageFormatter';
import { styles } from './SendMessageForm.styles';

interface SendMessageFormProps {
  roomId: string;
  placeholder?: string;
  maxLength?: number;
  disabled?: boolean;
  onSend?: (message: string) => void;
}

export const SendMessageForm: React.FC<SendMessageFormProps> = ({
  roomId,
  placeholder = '메시지를 입력하세요...',
  maxLength = 500,
  disabled = false,
  onSend,
}) => {
  const [message, setMessage] = useState('');
  const [errors, setErrors] = useState<string[]>([]);
  const inputRef = useRef<any>(null);

  const sendMessage = useSendMessage({
    onSuccess: () => {
      setMessage('');
      setErrors([]);
      onSend?.(message);
    },
    onError: (error) => {
      setErrors([error.message]);
      Alert.alert('오류', error.message);
    },
  });

  const handleSend = () => {
    if (disabled || sendMessage.isPending) return;

    // 유효성 검증
    const validation = messageValidation.validateMessage(message);
    if (!validation.isValid) {
      setErrors(validation.errors);
      Alert.alert('입력 오류', validation.errors.join('\n'));
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

  const handleChange = (value: string) => {
    if (value.length <= maxLength) {
      setMessage(value);
      setErrors([]); // 입력 시 에러 클리어
    }
  };

  const isSubmitDisabled = disabled || sendMessage.isPending || message.trim().length === 0;

  return (
    <View style={styles.container}>
      <View style={styles.inputContainer}>
        <Input
          ref={inputRef}
          value={message}
          onChangeText={handleChange}
          placeholder={placeholder}
          placeholderTextColor="#8e8e93"
          editable={!disabled && !sendMessage.isPending}
          multiline={true}
          numberOfLines={1}
          maxLength={maxLength}
          style={[
            styles.input,
            errors.length > 0 && styles.inputError
          ]}
          onSubmitEditing={handleSend}
          returnKeyType="send"
          blurOnSubmit={false}
          autoCorrect={false}
          autoCapitalize="none"
          keyboardType="default"
        />
        
        <View style={styles.actions}>
            <Label style={styles.characterCount}>
              {message.length}/{maxLength}
            </Label>
            
            <Button
              onPress={handleSend}
              disabled={isSubmitDisabled}
              style={[
                styles.sendButton,
                ...(isSubmitDisabled ? [styles.sendButtonDisabled] : [])
              ]}
            >
              <Label style={[
                styles.sendButtonText,
                ...(isSubmitDisabled ? [styles.sendButtonTextDisabled] : [])
              ]}>
                {sendMessage.isPending ? '⏳' : '↑'}
              </Label>
            </Button>
        </View>
      </View>

      {errors.length > 0 && (
        <View style={styles.errorContainer}>
          {errors.map((error, index) => (
            <Label key={index} style={styles.errorText}>
              {error}
            </Label>
          ))}
        </View>
      )}
    </View>
  );
};