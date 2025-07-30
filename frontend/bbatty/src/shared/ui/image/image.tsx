import React from 'react';
import { Image as RNImage, ImageProps as RNImageProps } from 'react-native';

export const Image: React.FC<RNImageProps> = (props) => {
  return <RNImage {...props} />;
};
