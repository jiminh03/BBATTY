// post/ui/PostForm.tsx
import React, { useState } from 'react';
import { View, TextInput, Button, Text, StyleSheet } from 'react-native';
import { useCreatePost } from '../queries/usePostQueries';
import { isValidPost, validatePostContent } from '../utils/vaildation';

export const PostForm: React.FC = () => {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [error, setError] = useState('');
  const createPost = useCreatePost();

  const handleSubmit = () => {
    const validationMessage = validatePostContent(title, content);
    if (!isValidPost({ title, content })) {
      setError(validationMessage ?? '');
      return;
    }

    createPost.mutate(
      { title, content, teamId: 1 },
      {
        onSuccess: () => {
          setTitle('');
          setContent('');
          setError('');
        },
        onError: () => {
          setError('게시글 생성 실패');
        },
      }
    );
  };

  return (
    <View style={styles.container}>
      <TextInput
        style={styles.input}
        placeholder="제목"
        value={title}
        onChangeText={setTitle}
      />
      <TextInput
        style={[styles.input, styles.textArea]}
        placeholder="내용"
        value={content}
        onChangeText={setContent}
        multiline
      />
      {error !== '' && <Text style={styles.error}>{error}</Text>}
      <Button title="게시글 생성" onPress={handleSubmit} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 16,
    backgroundColor: '#fff',
    flex: 1,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 4,
    padding: 12,
    marginBottom: 12,
  },
  textArea: {
    height: 120,
    textAlignVertical: 'top',
  },
  error: {
    color: 'red',
    marginBottom: 8,
  },
});
