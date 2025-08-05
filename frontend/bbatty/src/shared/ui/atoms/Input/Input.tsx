import React, { forwardRef } from "react";
import { TextInput, TextInputProps, Platform } from "react-native";

export type InputProps = TextInputProps;

export const Input = forwardRef<TextInput, InputProps>((props, ref) => {
  return (
    <TextInput 
      ref={ref} 
      {...props}
      // Android 한글 입력 이슈
      autoCorrect={props.autoCorrect ?? false}
      autoCapitalize={props.autoCapitalize ?? "none"}
      keyboardType={props.keyboardType ?? "default"}
      // Android에서 한글 입력 시 텍스트가 보이지 않는 문제
      style={[
        Platform.OS === 'android' && {
          fontFamily: 'System',
        },
        props.style
      ]}
    />
  );
});

Input.displayName = "Input";