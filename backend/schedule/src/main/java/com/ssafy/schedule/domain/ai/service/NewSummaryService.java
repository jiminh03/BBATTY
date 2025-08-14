package com.ssafy.schedule.domain.ai.service;

import com.ssafy.schedule.domain.ai.dto.NewsList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewSummaryService {

    private final ChatModel chatModel;

    /**
     * 뉴스 기사를 받아 3줄 요약문을 반환한다.
     *
     * @param articleText 뉴스 기사 원문
     * @return 요약된 문자열
     */
    public NewsList summarize(String articleText, String teamName) {

        // 1) OpenAI 구조화 출력: JSON_SCHEMA 강제
        String schema = """
        {
          "type": "object",
          "properties": {
            "items": {
              "type": "array",
              "maxItems": 5,
              "items": {
                "type": "object",
                "properties": {
                  "title":   { "type": "string" },
                  "summary": { "type": "string" }
                },
                "required": ["title","summary"],
                "additionalProperties": false
              }
            }
          },
          "required": ["items"],
          "additionalProperties": false
        }
        """;

        ResponseFormat rf = ResponseFormat.builder()
                .type(ResponseFormat.Type.JSON_SCHEMA)
                .jsonSchema(schema)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder().build();
        options.setResponseFormat(rf);


        // 1. 지시문 (system message)
        String systemInstruction = """
            당신은 야구 커뮤니티 상단에 배치할 요약 블록을 작성하는 AI입니다.
            규칙:
            1) 입력된 여러 뉴스 중 '%s'가 '주 내용'인 서로 다른 내용의 기사를 최대 5개 고르세요.
            2) 제목을 봤을 때 다른 팀이 아닌 %s가 주인공인 기사로 골라야합니다.
            3) title은 1줄, summary은 2줄로 완성된 문장으로 정리하세요.
            4) 반드시 제공된 JSON 스키마와 '완전히 일치'하는 JSON으로만 답하세요.
            5) 만든 문장들이 유사한 내용을 가지고 있다면 하나만 제시하고 각기 다른 내용으로 제시하세요.
        """.formatted(teamName, teamName);

        // 2. 사용자 입력 (user message)
        String userQuestion = "다음은 뉴스 기사 원문 목록입니다:\n" + articleText;

        // 3. Prompt 구성
        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage(systemInstruction),
                        new UserMessage(userQuestion)
                ),
                options
        );

        // 3. 호출
        ChatResponse response = chatModel.call(prompt);
        log.info("[chatResponse 디버깅] : {}",response.getResult().getOutput().getText());
        String json = response.getResult().getOutput().getText();

        // 4) 문자열 → 자바 객체 파싱 (Spring AI 구조화 출력 컨버터)
        BeanOutputConverter<NewsList> converter = new BeanOutputConverter<>(NewsList.class);
        NewsList result = converter.convert(json); // 스키마 어긋나면 여기서 예외

        return result;
    }
}
