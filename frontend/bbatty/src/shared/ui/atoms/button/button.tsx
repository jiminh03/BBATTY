import React from 'react';
import { Pressable, PressableProps, ViewStyle } from 'react-native';

interface ButtonProps extends Omit<PressableProps, 'onPress'> {
  onPress: () => void;
  disabled?: boolean;
  children: React.ReactNode;
  style?: ViewStyle | ViewStyle[];
}

export const Button: React.FC<ButtonProps> = ({ 
  onPress, 
  disabled = false, 
  children, 
  style,
  ...props 
}) => {
  return (
    <Pressable 
      onPress={onPress} 
      disabled={disabled}
      style={[
        style,
        disabled && { opacity: 0.5 }
      ]}
      {...props}
    >
      {children}
    </Pressable>
  );
};