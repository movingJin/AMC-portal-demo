package com.backend.amc_portal.board.service;

import com.backend.amc_portal.auth.entity.User;
import com.backend.amc_portal.auth.repository.UserRepository;
import com.backend.amc_portal.board.dto.BoardFileResponse;
import com.backend.amc_portal.board.dto.BoardRequest;
import com.backend.amc_portal.board.dto.BoardResponse;
import com.backend.amc_portal.board.entity.Board;
import com.backend.amc_portal.board.entity.BoardFile;
import com.backend.amc_portal.board.entity.BoardFileHistory;
import com.backend.amc_portal.board.entity.BoardMaster;
import com.backend.amc_portal.board.enums.BoardFileEventType;
import com.backend.amc_portal.board.repository.BoardFileHistoryRepository;
import com.backend.amc_portal.board.repository.BoardFileRepository;
import com.backend.amc_portal.board.repository.BoardMasterRepository;
import com.backend.amc_portal.board.repository.BoardRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BoardService {

  private final BoardRepository boardRepository;
  private final BoardMasterRepository boardMasterRepository;
  private final UserRepository userRepository;
  private final BoardFileRepository boardFileRepository;
  private final BoardFileHistoryRepository boardFileHistoryRepository;

  @Value("${app.file.upload-dir}")
  private String uploadDir;

  @Value("${app.file.max-size-bytes}")
  private long maxSizeBytes;

  @Transactional(readOnly = true)
  public Page<BoardResponse> list(String keyword, Long boardMasterId, Pageable pageable) {
    return boardRepository.search(keyword, boardMasterId, pageable).map(BoardResponse::summary);
  }

  @Transactional(readOnly = true)
  public BoardResponse get(Long id) {
    Board b = boardRepository.findById(id)
        .filter(board -> !board.isDeleted())
        .orElseThrow(() -> ApiException.notFound("게시글을 찾을 수 없습니다."));
    List<BoardFileResponse> files = boardFileRepository.findByBoardIdOrderByIdAsc(id).stream()
        .map(BoardFileResponse::from)
        .toList();
    return BoardResponse.from(b, files);
  }

  @Transactional
  public void incrementView(Long id) {
    boardRepository.findById(id).ifPresent(Board::incrementViewCount);
  }

  @Transactional
  public BoardResponse create(Long userId, BoardRequest req, List<MultipartFile> files) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> ApiException.unauthorized("사용자를 찾을 수 없습니다."));
    BoardMaster boardMaster = boardMasterRepository.findById(req.boardMasterId())
        .orElseThrow(() -> ApiException.notFound("게시판을 찾을 수 없습니다."));
    Board board = boardRepository.save(Board.builder()
        .title(req.title())
        .content(req.content())
        .author(user)
        .boardMaster(boardMaster)
        .build());

    List<BoardFileResponse> savedFiles = persistFiles(board, files, boardMaster, user);
    return BoardResponse.from(board, savedFiles);
  }

  @Transactional
  public BoardResponse update(Long userId, Long boardId, BoardRequest req, List<MultipartFile> files) {
    Board board = boardRepository.findById(boardId)
        .filter(b -> !b.isDeleted())
        .orElseThrow(() -> ApiException.notFound("게시글을 찾을 수 없습니다."));
    checkOwner(board.getAuthor().getId(), userId);
    board.update(req.title(), req.content());

    List<Long> deleteIds = req.deleteFileIds() != null ? req.deleteFileIds() : List.of();
    deleteIds.forEach(fileId -> boardFileRepository.findById(fileId).ifPresent(bf -> {
      saveDeleteHistory(bf, board);
      boardFileRepository.delete(bf);
    }));

    persistFiles(board, files, board.getBoardMaster(), board.getAuthor());

    List<BoardFileResponse> remaining = boardFileRepository.findByBoardIdOrderByIdAsc(boardId)
        .stream().map(BoardFileResponse::from).toList();
    return BoardResponse.from(board, remaining);
  }

  @Transactional
  public void delete(Long userId, Long boardId) {
    Board board = boardRepository.findById(boardId)
        .filter(b -> !b.isDeleted())
        .orElseThrow(() -> ApiException.notFound("게시글을 찾을 수 없습니다."));
    checkOwner(board.getAuthor().getId(), userId);
    board.softDelete();
  }

  private List<BoardFileResponse> persistFiles(
      Board board, List<MultipartFile> files, BoardMaster boardMaster, User actor) {
    if (files.isEmpty()) return List.of();
    if (boardMaster == null || !boardMaster.isFileYn()) {
      throw ApiException.badRequest("이 게시판은 파일 첨부를 지원하지 않습니다.");
    }
    int currentCount = boardFileRepository.findByBoardIdOrderByIdAsc(board.getId()).size();
    if (currentCount + files.size() > boardMaster.getFileMaxCount()) {
      throw ApiException.badRequest(
          "첨부파일은 최대 " + boardMaster.getFileMaxCount() + "개까지 등록할 수 있습니다.");
    }
    return files.stream().map(f -> saveFile(board, f, actor)).map(BoardFileResponse::from).toList();
  }

  private BoardFile saveFile(Board board, MultipartFile file, User actor) {
    if (file.isEmpty()) throw ApiException.badRequest("빈 파일은 업로드할 수 없습니다.");
    if (file.getSize() > maxSizeBytes) {
      throw ApiException.badRequest(
          "파일 크기는 " + (maxSizeBytes / 1024 / 1024) + "MB를 초과할 수 없습니다.");
    }
    String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
    String ext = originalName.contains(".")
        ? originalName.substring(originalName.lastIndexOf('.')) : "";
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
    String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
    String storagePath = dest.toAbsolutePath().toString();
    BoardFile boardFile = boardFileRepository.save(BoardFile.builder()
        .board(board).originalName(originalName).storedName(storedName)
        .contentType(contentType).fileSize(file.getSize()).storagePath(storagePath).build());
    boardFileHistoryRepository.save(BoardFileHistory.builder()
        .fileId(boardFile.getId()).board(board).eventType(BoardFileEventType.UPLOAD)
        .originalName(originalName).storedName(storedName).storagePath(storagePath)
        .fileSize(file.getSize()).contentType(contentType).actedBy(actor).build());
    return boardFile;
  }

  private void saveDeleteHistory(BoardFile bf, Board board) {
    boardFileHistoryRepository.save(BoardFileHistory.builder()
        .fileId(bf.getId()).board(board).eventType(BoardFileEventType.DELETE)
        .originalName(bf.getOriginalName()).storedName(bf.getStoredName())
        .storagePath(bf.getStoragePath()).fileSize(bf.getFileSize())
        .contentType(bf.getContentType()).actedBy(board.getAuthor()).build());
  }

  private void checkOwner(Long ownerId, Long actorId) {
    if (!ownerId.equals(actorId)) throw ApiException.forbidden("권한이 없습니다.");
  }
}
