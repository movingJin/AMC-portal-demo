package com.backend.amc_portal.board.service;

import com.backend.amc_portal.auth.entity.User;
import com.backend.amc_portal.auth.repository.UserRepository;
import com.backend.amc_portal.board.dto.BoardFileDownloadResponse;
import com.backend.amc_portal.board.dto.BoardFileHistoryResponse;
import com.backend.amc_portal.board.dto.BoardFileResponse;
import com.backend.amc_portal.board.entity.*;
import com.backend.amc_portal.board.enums.BoardFileEventType;
import com.backend.amc_portal.board.repository.*;
import com.backend.amc_portal.common.exception.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BoardFileService {

  private final BoardFileRepository boardFileRepository;
  private final BoardFileHistoryRepository historyRepository;
  private final BoardFileDownloadRepository downloadRepository;
  private final BoardRepository boardRepository;
  private final UserRepository userRepository;

  @Value("${app.file.upload-dir}")
  private String uploadDir;

  @Value("${app.file.max-size-bytes}")
  private long maxSizeBytes;

  @Transactional(readOnly = true)
  public List<BoardFileResponse> list(Long boardId) {
    return boardFileRepository.findByBoardIdOrderByIdAsc(boardId).stream()
        .map(BoardFileResponse::from)
        .toList();
  }

  @Transactional
  public List<BoardFileResponse> upload(Long userId, Long boardId, List<MultipartFile> files) {
    Board board = findBoard(boardId);
    checkOwner(board, userId);

    var boardMaster = board.getBoardMaster();
    if (boardMaster == null || !boardMaster.isFileYn()) {
      throw ApiException.badRequest("이 게시판은 파일 첨부를 지원하지 않습니다.");
    }

    int currentCount = boardFileRepository.findByBoardIdOrderByIdAsc(boardId).size();
    int maxCount = boardMaster.getFileMaxCount();
    if (currentCount + files.size() > maxCount) {
      throw ApiException.badRequest(
          "첨부파일은 최대 " + maxCount + "개까지 등록할 수 있습니다. (현재 " + currentCount + "개)");
    }

    User actor = board.getAuthor();
    return files.stream()
        .map(file -> saveFile(board, file, actor))
        .map(BoardFileResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<BoardFileHistoryResponse> listHistory(Long userId, Long boardId) {
    Board board = findBoard(boardId);
    checkOwner(board, userId);
    return historyRepository.findByBoardIdWithActedBy(boardId).stream()
        .map(BoardFileHistoryResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<BoardFileDownloadResponse> listDownloads(Long userId, Long boardId) {
    Board board = findBoard(boardId);
    checkOwner(board, userId);
    return downloadRepository.findByBoardIdWithUser(boardId).stream()
        .map(BoardFileDownloadResponse::from)
        .toList();
  }

  public record DownloadResult(Resource resource, String originalName, String contentType) {}

  @Transactional
  public DownloadResult download(Long fileId, Long userId, String ipAddress) {
    BoardFile boardFile = findFile(fileId);
    try {
      Path path = Paths.get(boardFile.getStoragePath());
      Resource resource = new UrlResource(path.toUri());
      if (!resource.exists() || !resource.isReadable()) {
        throw ApiException.notFound("파일을 찾을 수 없습니다.");
      }

      User user =
          userRepository
              .findById(userId)
              .orElseThrow(() -> ApiException.unauthorized("사용자를 찾을 수 없습니다."));
      downloadRepository.save(
          BoardFileDownload.builder()
              .fileId(boardFile.getId())
              .board(boardFile.getBoard())
              .originalName(boardFile.getOriginalName())
              .storedName(boardFile.getStoredName())
              .storagePath(boardFile.getStoragePath())
              .user(user)
              .ipAddress(ipAddress)
              .build());

      return new DownloadResult(resource, boardFile.getOriginalName(), boardFile.getContentType());
    } catch (IOException e) {
      throw ApiException.notFound("파일을 읽을 수 없습니다.");
    }
  }

  @Transactional
  public void delete(Long userId, Long fileId) {
    BoardFile boardFile = findFile(fileId);
    checkOwner(boardFile.getBoard(), userId);

    historyRepository.save(
        BoardFileHistory.builder()
            .fileId(boardFile.getId())
            .board(boardFile.getBoard())
            .eventType(BoardFileEventType.DELETE)
            .originalName(boardFile.getOriginalName())
            .storedName(boardFile.getStoredName())
            .storagePath(boardFile.getStoragePath())
            .fileSize(boardFile.getFileSize())
            .contentType(boardFile.getContentType())
            .actedBy(boardFile.getBoard().getAuthor())
            .build());

    boardFileRepository.delete(boardFile);
  }

  private BoardFile saveFile(Board board, MultipartFile file, User actor) {
    if (file.isEmpty()) throw ApiException.badRequest("빈 파일은 업로드할 수 없습니다.");
    if (file.getSize() > maxSizeBytes) {
      throw ApiException.badRequest("파일 크기는 " + (maxSizeBytes / 1024 / 1024) + "MB를 초과할 수 없습니다.");
    }

    String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
    String ext =
        originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "";
    String storedName = UUID.randomUUID() + ext;

    Path dir = Paths.get(uploadDir, String.valueOf(board.getId()));
    Path dest = dir.resolve(storedName);

    try {
      Files.createDirectories(dir);
      try (InputStream in = file.getInputStream()) {
        Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new RuntimeException("파일 저장에 실패했습니다.", e);
    }

    String contentType =
        file.getContentType() != null ? file.getContentType() : "application/octet-stream";
    String storagePath = dest.toAbsolutePath().toString();

    BoardFile boardFile =
        boardFileRepository.save(
            BoardFile.builder()
                .board(board)
                .originalName(originalName)
                .storedName(storedName)
                .contentType(contentType)
                .fileSize(file.getSize())
                .storagePath(storagePath)
                .build());

    historyRepository.save(
        BoardFileHistory.builder()
            .fileId(boardFile.getId())
            .board(board)
            .eventType(BoardFileEventType.UPLOAD)
            .originalName(originalName)
            .storedName(storedName)
            .storagePath(storagePath)
            .fileSize(file.getSize())
            .contentType(contentType)
            .actedBy(actor)
            .build());

    return boardFile;
  }

  private Board findBoard(Long boardId) {
    return boardRepository
        .findById(boardId)
        .orElseThrow(() -> ApiException.notFound("게시글을 찾을 수 없습니다."));
  }

  private BoardFile findFile(Long fileId) {
    return boardFileRepository
        .findById(fileId)
        .orElseThrow(() -> ApiException.notFound("파일을 찾을 수 없습니다."));
  }

  private void checkOwner(Board board, Long userId) {
    if (!board.getAuthor().getId().equals(userId)) {
      throw ApiException.forbidden("권한이 없습니다.");
    }
  }
}
