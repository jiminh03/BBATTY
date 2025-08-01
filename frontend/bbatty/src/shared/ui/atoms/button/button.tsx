import React from 'react';
import { Pressable } from 'react-native';

interface ButtonProps {
  onPress: () => void;
  disabled?: boolean;
  children: React.ReactNode;
}

export const Button: React.FC<ButtonProps> = ({
  onPress,
  disabled = false,
  children,
}) => {
  return (
    <Pressable onPress={onPress} disabled={disabled}>
      {children}
    </Pressable>
  );
};
