package com.capstone.backend.controller;

import com.capstone.backend.dto.VoiceProfileResponseDto;
import com.capstone.backend.entity.User;
import com.capstone.backend.entity.VoiceProfile;
import com.capstone.backend.repository.UserRepository;
import com.capstone.backend.repository.VoiceProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ContentDisposition; // [추가]
import java.nio.charset.StandardCharsets; // [추가]

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/voice-profiles")
@RequiredArgsConstructor
public class VoiceProfileController {

    private final VoiceProfileRepository voiceProfileRepository;
    private final UserRepository userRepository;

    // 1. [앱용] 목소리 등록 API
    // 앱에서 파일을 업로드하면 DB에 저장하고 ID를 반환합니다.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> uploadVoiceProfile(
            Principal principal,
            @RequestParam("profileName") String profileName,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String userPhoneNumber = principal.getName();
        User user = userRepository.findByPhoneNumber(userPhoneNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));

        VoiceProfile voiceProfile = VoiceProfile.builder()
                .user(user)
                .profileName(profileName)
                .voiceData(file.getBytes()) // 파일의 이진 데이터를 바로 저장
                .build();

        VoiceProfile savedProfile = voiceProfileRepository.save(voiceProfile);

        // 생성된 ID(voice_profile_id)를 반환 -> 앱이나 GPU에게 이 번호를 알려주면 됨
        return ResponseEntity.ok(savedProfile.getId());
    }

    // 2. [GPU용] 목소리 데이터 조회 API
    // GPU가 "ID 5번 목소리 내놔"라고 요청하면 이진 데이터를 스트리밍해줍니다.
    @GetMapping("/{voiceProfileId}/download")
    public ResponseEntity<byte[]> downloadVoiceData(@PathVariable Long voiceProfileId) {

        VoiceProfile voiceProfile = voiceProfileRepository.findById(voiceProfileId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        // 한글 파일명 깨짐 방지를 위해 ContentDisposition 빌더 사용 (RFC 5987 표준 지원)
        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(voiceProfile.getProfileName() + ".mp3", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(voiceProfile.getVoiceData());
    }

    // 3. [앱용] 내 목소리 목록 조회 API (누락된 부분 추가)
    @GetMapping("/me")
    public ResponseEntity<List<VoiceProfileResponseDto>> getUserVoiceProfiles(Principal principal) {
        String userPhoneNumber = principal.getName();
        User user = userRepository.findByPhoneNumber(userPhoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // user.getId()를 사용하여 조회
        List<VoiceProfile> profiles = voiceProfileRepository.findAllByUserId(user.getId());

        List<VoiceProfileResponseDto> result = profiles.stream()
                .map(VoiceProfileResponseDto::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // 4. [GPU/앱용] 목소리 정보(메타데이터) 조회 API (신규 추가)
    // 파일 내용 없이 ID, 이름, 생성일만 가볍게 반환합니다.
    @GetMapping("/{voiceProfileId}")
    public ResponseEntity<VoiceProfileResponseDto> getVoiceProfileInfo(@PathVariable Long voiceProfileId) {
        VoiceProfile voiceProfile = voiceProfileRepository.findById(voiceProfileId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        return ResponseEntity.ok(new VoiceProfileResponseDto(voiceProfile));
    }
}
