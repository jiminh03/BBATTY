import React from 'react';
import { Image as RNImage, ImageProps as RNImageProps } from 'react-native';

// 추후 확장 시 필요
// export interface ImageProps extends RNImageProps {}

export const Image: React.FC<RNImageProps> = (props) => {
  return <RNImage {...props} />;
};