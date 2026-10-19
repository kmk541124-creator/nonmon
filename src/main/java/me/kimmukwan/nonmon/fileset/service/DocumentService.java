package me.kimmukwan.nonmon.fileset.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import me.kimmukwan.nonmon.fileset.dto.response.OllamaResponse;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

@Service
public class DocumentService {
    private final Tika tika = new Tika();
    private final RestClient restClient = RestClient.create();
    private final String model;
    private final String ollamaUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentService(@Value("${ollama.base-url}") String ollamaUrl,
                           @Value("${ollama.model}") String model){
        this.ollamaUrl = ollamaUrl;
        this.model = model;
    }

    public OllamaResponse analyze(MultipartFile file) throws IOException, TikaException{
        String text;
        try(InputStream in = file.getInputStream()){
            text = tika.parseToString(in);
        }

        String prompt = """
        너는 한국어 문서를 검토하는 교정 전문가다.
        아래 [문서]를 읽고 요약, 개선 방향, 맞춤법 오류를 분석해서 반드시 아래 JSON 형식으로만 답하라.
        JSON 앞뒤에 다른 문장, 설명, 코드 블록 표시는 절대 붙이지 마라.

        {"check": "<요약 내용>", "mat": [{"wrong": "<틀린 표현>", "right": "<고친 표현>"}], "feature": "<개선 방향 내용>"}

        위 형식에서 <> 안의 글자는 무엇을 쓸 자리인지 알려 주는 설명일 뿐이다.
        "<요약 내용>", "<개선 방향 내용>", "요약", "개선 방향" 같은 설명 글자를 값으로 그대로 쓰지 말고, 반드시 분석한 실제 문장으로 채워라.
        check, mat, feature 순서대로 작성한다. feature는 문서 전체를 검토한 뒤 마지막에 작성한다.

        [공통 규칙]
        1. check와 feature는 모두 "~습니다" 체로 쓴다.
        2. 문서에 없는 내용, 인물, 호칭, 수치를 지어내지 않는다.

        [check 작성 규칙]
        1. 문서의 중심 내용을 처음부터 끝까지의 흐름에 맞춰 3~4문장으로 요약한다.

        [feature 작성 규칙]
        1. 먼저 문서의 종류, 글쓴이, 읽을 사람을 구분해서 파악한다. 글쓴이와 읽을 사람은 서로 다른 사람이다.
           예: 동아리 활동 보고서라면 글쓴이는 동아리 구성원이고, 읽을 사람은 지도 교사나 학교다.
        2. 읽을 사람의 입장에서 이 문서에 부족한 내용, 논리가 어긋나는 부분, 어색한 문장을 찾는다.
        3. 지적은 2~4개로 하고, 각 지적마다 문서의 해당 문장을 작은따옴표로 짧게 인용한 뒤 무엇이 문제인지와 어떻게 고치면 좋을지를 쓴다.
        4. "다소 어색하다", "개선이 필요하다"처럼 어느 부분인지 알 수 없는 막연한 표현만으로 끝내지 않는다.
        5. feature는 최소 4문장 이상의 완성된 글이어야 한다. 빈 값, 제목만 있는 값, 한 단어짜리 값은 절대 허용되지 않는다.
        6. feature 작성 예시(형식만 참고하고 내용은 따라 하지 마라):
           이 문서는 행사 안내문으로, 학부모가 읽는 글입니다. '참가비는 추후 공지'라는 문장은 금액과 공지 시기가 없어 읽는 사람이 준비하기 어렵습니다. 예상 금액과 공지 날짜를 함께 적는 것이 좋습니다. 또한 '장소는 학교'라는 문장은 강당인지 운동장인지 알 수 없으므로 구체적인 장소를 적어야 합니다.

        [mat 작성 규칙]
        1. 검사는 두 번에 나누어 한다.
           첫 번째: 첫 문장부터 마지막 문장까지 한 문장씩 순서대로 읽으며 철자 오류를 찾는다.
           두 번째: 다시 첫 문장부터 읽으며 띄어쓰기 오류만 찾는다.
        2. 띄어쓰기 검사에서는 특히 아래를 확인한다.
           - "마무리하다", "정리하다", "공부하다"처럼 명사에 "하다"가 붙은 말은 붙여 쓴다.
           - "수", "것", "데", "만큼", "뿐" 같은 의존 명사는 앞말과 띄어 쓴다.
           - "안 되다", "못 하다"처럼 부정을 나타내는 "안", "못"은 뒷말과 띄어 쓴다.
        3. 아래는 자주 틀리는 표현의 예시다. 이 예시에 없는 오류도 모두 찾아야 한다.
           - 몇일, 몇 일 → 며칠 ("몇 일"은 띄어 써도 틀린 표기다)
           - 금새 → 금세 / 왠만하면 → 웬만하면 / 어의없다 → 어이없다
           - 됬다 → 됐다 / 되요 → 돼요 / 않되다 → 안 되다
           - 오랫만에 → 오랜만에 / 설겆이 → 설거지 / 희안하다 → 희한하다 / 일부로 → 일부러
        4. right에는 wrong과 같은 뜻의 올바른 표기만 쓴다. 다른 단어로 바꾸거나 문장을 다시 쓰지 않는다.
        5. wrong에는 문서에 실제로 적힌 글자를 띄어쓰기까지 그대로 옮겨 적는다.
        6. 맞게 쓴 표현은 mat에 넣지 않는다. 오류가 없으면 mat을 빈 배열 []로 둔다.
        7. 같은 오류가 여러 번 나오면 한 번만 넣는다.
        8. 답을 내기 전에 mat의 각 right가 표준국어대사전에 실린 올바른 표기인지 한 번 더 확인하고, 확신이 없는 항목은 빼라.

        [문서]
        %s
        """.formatted(text);

        OllamaR response = restClient.post()
                .uri(ollamaUrl + "/api/generate")
                .body(Map.of(
                        "model", model,
                        "prompt", prompt,
                        "format", "json",
                        "stream", false
                ))
                .retrieve()
                .body(OllamaR.class);

        if(response == null){
            throw new IllegalArgumentException("ollama 응답이 없음");
        }

        try{
            return objectMapper.readValue(response.response, OllamaResponse.class);
        } catch (Exception e){
            throw new IllegalArgumentException("ollama가 응답을 해석하지 못함 " + response.response, e);
        }
    }

    @JsonIgnoreProperties
    private record OllamaR(String response){

    }
}
