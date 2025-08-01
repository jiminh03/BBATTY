import React, { forwardRef } from "react";
import { TextInput, TextInputProps } from "react-native";

export type InputProps = TextInputProps;

export const Input = forwardRef<TextInput, InputProps>((props, ref) => {
  return <TextInput ref={ref} {...props} />;
});

Input.displayName = "Input";