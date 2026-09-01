package com.example.jejugilmoa.global.external.tourapi;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * KorService2 cat3 코드 → 소분류명 변환기.
 * KorService2 areaBasedList2는 구분류체계(A01010800 형식) 코드를 반환하므로
 * OLD_CAT3_LABELS 정적 맵으로 1차 조회하고, 신분류체계 코드는 CSV 기반으로 폴백.
 */
@Slf4j
@Component
public class ClsSystem3Resolver {

    // KorService2 cat2 (중분류) 코드 → 중분류명
    private static final Map<String, String> OLD_CAT2_LABELS = Map.ofEntries(
        Map.entry("A0101", "자연관광지"),
        Map.entry("A0102", "동식물관광지"),
        Map.entry("A0201", "역사관광지"),
        Map.entry("A0202", "휴양관광지"),
        Map.entry("A0203", "체험관광지"),
        Map.entry("A0204", "산업관광지"),
        Map.entry("A0205", "건축/조형물"),
        Map.entry("A0206", "문화시설"),
        Map.entry("A0301", "육상 레포츠"),
        Map.entry("A0302", "수상 레포츠"),
        Map.entry("A0303", "항공 레포츠"),
        Map.entry("A0304", "복합 레포츠"),
        Map.entry("A0401", "음식점"),
        Map.entry("A0402", "카페/찻집"),
        Map.entry("A0501", "쇼핑몰"),
        Map.entry("A0502", "상점"),
        Map.entry("B0201", "관광호텔"),
        Map.entry("B0202", "콘도미니엄"),
        Map.entry("B0204", "펜션"),
        Map.entry("B0206", "민박"),
        Map.entry("B0207", "게스트하우스"),
        Map.entry("B0210", "야영장/캠핑")
    );

    // KorService2 areaBasedList2가 반환하는 구분류체계 코드 → 소분류명
    private static final Map<String, String> OLD_CAT3_LABELS = Map.ofEntries(
        // 자연관광지 (A0101)
        Map.entry("A01010100", "국립공원"),
        Map.entry("A01010200", "도립공원"),
        Map.entry("A01010300", "군립공원"),
        Map.entry("A01010400", "자연생태관광지"),
        Map.entry("A01010600", "자연휴양림"),
        Map.entry("A01010700", "수목원"),
        Map.entry("A01010800", "공원"),
        Map.entry("A01010900", "저수지"),
        Map.entry("A01011000", "강"),
        Map.entry("A01011100", "계곡"),
        Map.entry("A01011200", "해수욕장"),
        Map.entry("A01011300", "섬"),
        Map.entry("A01011400", "폭포"),
        Map.entry("A01011500", "호수"),
        Map.entry("A01011600", "온천/스파"),
        Map.entry("A01011700", "동굴"),
        Map.entry("A01011800", "산/오름"),
        Map.entry("A01011900", "항구/어촌"),
        // 동식물관광지 (A0102)
        Map.entry("A01020100", "동물원"),
        Map.entry("A01020200", "식물원"),
        Map.entry("A01020300", "해양관광지"),
        Map.entry("A01020400", "농업관광지"),
        Map.entry("A01020500", "어촌체험"),
        // 문화/역사 (A02)
        Map.entry("A02010100", "사찰"),
        Map.entry("A02010200", "서원/향교"),
        Map.entry("A02010500", "유적지/사적지"),
        Map.entry("A02010600", "문화유적지"),
        Map.entry("A02020100", "박물관"),
        Map.entry("A02020200", "기념관"),
        Map.entry("A02020300", "전시관"),
        Map.entry("A02020500", "공연장"),
        Map.entry("A02020600", "미술관"),
        Map.entry("A02020700", "테마공원"),
        Map.entry("A02020800", "수족관"),
        Map.entry("A02030200", "공예/특산물"),
        // 레포츠 (A03)
        Map.entry("A03010100", "레포츠"),
        Map.entry("A03010300", "스노클링"),
        Map.entry("A03010400", "수영"),
        Map.entry("A03010500", "래프팅"),
        Map.entry("A03010600", "암벽등반"),
        Map.entry("A03020100", "골프"),
        Map.entry("A03020300", "승마"),
        Map.entry("A03030100", "낚시"),
        // 음식 (A04)
        Map.entry("A04010100", "한식"),
        Map.entry("A04010200", "서양식"),
        Map.entry("A04010300", "일식"),
        Map.entry("A04010400", "중식"),
        Map.entry("A04010500", "이색음식점"),
        Map.entry("A04010700", "카페/찻집"),
        Map.entry("A04011100", "횟집"),
        Map.entry("A04011200", "한정식"),
        // 쇼핑 (A05)
        Map.entry("A05010100", "복합 쇼핑몰"),
        Map.entry("A05010200", "상설시장"),
        Map.entry("A05020100", "지역특산물"),
        Map.entry("A05020200", "토산품점")
    );

    // 신분류체계 코드 (AC010100 형식) — clsSystemTypeList.csv 기반
    private final Map<String, String> newCodeToName = new HashMap<>(256);

    @PostConstruct
    void init() {
        try (InputStream is = getClass().getResourceAsStream("/data/clsSystemTypeList.csv")) {
            if (is == null) {
                log.warn("clsSystemTypeList.csv 를 찾을 수 없음 — 신분류체계 코드 변환 비활성화");
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                int lineNo = 0;
                while ((line = reader.readLine()) != null) {
                    if (++lineNo <= 4) continue;
                    extractAndStore(line);
                }
            }
            log.info("clsSystem3 신분류 코드 {}개 로드 완료", newCodeToName.size());
        } catch (Exception e) {
            log.warn("clsSystemTypeList.csv 로드 실패 — 신분류체계 코드 변환 비활성화", e);
        }
    }

    /** cat3 코드 → 소분류명. 구분류 정적 맵 우선, 신분류 CSV 폴백. */
    public String resolve(String cat3Code) {
        if (cat3Code == null || cat3Code.isBlank()) return null;
        String code = cat3Code.trim();
        String label = OLD_CAT3_LABELS.get(code);
        return label != null ? label : newCodeToName.get(code);
    }

    /** cat2 코드 → 중분류명. */
    public String resolveMid(String cat2Code) {
        if (cat2Code == null || cat2Code.isBlank()) return null;
        return OLD_CAT2_LABELS.get(cat2Code.trim());
    }

    private void extractAndStore(String line) {
        int commaCount = 0, pos = 0;
        while (pos < line.length() && commaCount < 4) {
            if (line.charAt(pos++) == ',') commaCount++;
        }
        if (commaCount < 4 || pos >= line.length()) return;

        int codeStart = pos;
        while (pos < line.length() && line.charAt(pos) != ',') pos++;
        String code = line.substring(codeStart, pos).trim();
        if (code.isBlank() || pos >= line.length()) return;
        pos++;

        String name;
        if (pos < line.length() && line.charAt(pos) == '"') {
            pos++;
            int nameEnd = line.indexOf('"', pos);
            name = (nameEnd == -1) ? line.substring(pos) : line.substring(pos, nameEnd);
        } else {
            int nameEnd = line.indexOf(',', pos);
            name = (nameEnd == -1) ? line.substring(pos) : line.substring(pos, nameEnd);
        }
        name = name.trim();

        if (!code.isBlank() && !name.isBlank()) {
            newCodeToName.put(code, name);
        }
    }
}
