import React, { forwardRef } from "react";
import { Text, TextProps } from "react-native";

export type LabelProps = TextProps;

export const Label = forwardRef<Text, LabelProps>((props, ref) => {
  return <Text ref={ref} {...props} />;
});

Label.displayName = "Label";

export const truncateText = (text: string, maxLength: number) => {
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength) + '...';
};