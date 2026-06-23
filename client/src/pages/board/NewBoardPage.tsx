import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { FilePond, registerPlugin } from 'react-filepond'
import FilePondPluginImagePreview from 'filepond-plugin-image-preview'
import FilePondPluginFileValidateSize from 'filepond-plugin-file-validate-size'
import type { FilePondFile } from 'filepond'
import 'filepond/dist/filepond.min.css'
import 'filepond-plugin-image-preview/dist/filepond-plugin-image-preview.css'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth'
import RichTextEditor from '@/components/RichTextEditor'

registerPlugin(FilePondPluginImagePreview, FilePondPluginFileValidateSize)

const EMPTY_HTML_RE = /^(<p><\/p>)*$/

type BoardMasterMeta = { fileYn: boolean; fileMaxCount: number }
type BoardFile = { id: number; originalName: string; contentType: string; fileSize: number }

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function fileIcon(contentType: string): string {
  if (contentType.startsWith('image/')) return '🖼'
  if (contentType === 'application/pdf') return '📄'
  return '📎'
}

export default function NewBoardPage() {
  const { boardMasterId, postId } = useParams<{ boardMasterId: string; postId: string }>()
  const isEdit = !!postId
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [boardMaster, setBoardMaster] = useState<BoardMasterMeta | null>(null)
  const [editExistingFiles, setEditExistingFiles] = useState<BoardFile[]>([])
  const [pendingDeleteIds, setPendingDeleteIds] = useState<number[]>([])
  const [pondFiles, setPondFiles] = useState<FilePondFile[]>([])
  const [editFilesLoaded, setEditFilesLoaded] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const user = useAuth((s) => s.user)

  const pondRef = useRef<FilePond | null>(null)
  const stableFilesRef = useRef<FilePondFile[]>([])
  const approvedCountRef = useRef(0)
  const alertShownRef = useRef(false)

  useEffect(() => {
    api<BoardMasterMeta>(`/api/board-master/${boardMasterId}`)
      .then(setBoardMaster)
      .catch(() => {})
  }, [boardMasterId])

  useEffect(() => {
    if (!isEdit) return
    api<{ title: string; content: string; files?: BoardFile[] }>(`/api/board/${postId}`).then(
      (b) => {
        setTitle(b.title)
        setContent(b.content)
        setEditExistingFiles(b.files ?? [])
        setEditFilesLoaded(true)
      },
    )
  }, [postId])

  if (!user) {
    return <div className="card p-8 text-center text-ink-500">로그인이 필요합니다.</div>
  }

  const removeExistingFile = (fileId: number) => {
    setEditExistingFiles((prev) => prev.filter((f) => f.id !== fileId))
    setPendingDeleteIds((prev) => [...prev, fileId])
  }

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (EMPTY_HTML_RE.test(content.trim())) {
      setError('내용을 입력해주세요.')
      return
    }
    setError(null)
    setLoading(true)
    try {
      const buildFormData = (deleteFileIds: number[] = []) => {
        const formData = new FormData()
        formData.append(
          'data',
          new Blob(
            [
              JSON.stringify({
                title,
                content,
                boardMasterId: Number(boardMasterId),
                deleteFileIds,
              }),
            ],
            { type: 'application/json' },
          ),
        )
        for (const f of pondFiles) {
          formData.append('files', f.file, f.filename)
        }
        return formData
      }

      if (isEdit) {
        await api<void>(`/api/board/${postId}`, {
          method: 'PUT',
          body: buildFormData(pendingDeleteIds),
        })
        navigate(`/board/${boardMasterId}/post/${postId}`)
      } else {
        const created = await api<{ id: number }>('/api/board', {
          method: 'POST',
          body: buildFormData(),
        })
        navigate(`/board/${boardMasterId}/post/${created.id}`)
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : isEdit ? '수정 실패' : '작성 실패')
    } finally {
      setLoading(false)
    }
  }

  const showFileSection = boardMaster?.fileYn === true

  return (
    <div className="max-w-5xl mx-auto animate-fade-in">
      <div className="card p-8 space-y-5">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            {isEdit ? '글 수정' : '새 글 작성'}
          </h1>
          <p className="text-sm text-ink-500 mt-1">동료들과 공유할 내용을 적어주세요.</p>
        </div>
        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="label">제목</label>
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="제목을 입력하세요"
              className="input"
              maxLength={200}
              required
            />
          </div>
          <div>
            <label className="label">내용</label>
            <RichTextEditor value={content} onChange={setContent} />
          </div>

          {showFileSection && (!isEdit || editFilesLoaded) && (
            <div className="space-y-2">
              <label className="label">
                첨부파일
                <span className="ml-1 text-ink-400 font-normal">
                  (최대 {boardMaster!.fileMaxCount}개)
                </span>
              </label>

              {/* 기존 파일 목록 — 수정 모드에서만 표시 */}
              {isEdit && editExistingFiles.length > 0 && (
                <ul className="space-y-1">
                  {editExistingFiles.map((f) => (
                    <li
                      key={f.id}
                      className="flex items-center gap-3 px-3 py-2.5 bg-white border border-ink-100 rounded-lg"
                    >
                      <span className="text-lg shrink-0">{fileIcon(f.contentType)}</span>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-ink-800 truncate">
                          {f.originalName}
                        </p>
                        <p className="text-xs text-ink-400">{formatBytes(f.fileSize)}</p>
                      </div>
                      <button
                        type="button"
                        onClick={() => removeExistingFile(f.id)}
                        className="shrink-0 w-5 h-5 rounded-full bg-ink-300 hover:bg-red-500 flex items-center justify-center text-white text-[10px] transition-colors"
                        title="삭제"
                      >
                        ✕
                      </button>
                    </li>
                  ))}
                </ul>
              )}

              <FilePond
                ref={pondRef}
                instantUpload={false}
                onupdatefiles={(files) => {
                  stableFilesRef.current = files
                  approvedCountRef.current = 0
                  setPondFiles(files)
                }}
                allowMultiple={true}
                maxFileSize="10MB"
                labelIdle='파일을 드래그하거나 <span class="filepond--label-action">찾아보기</span>'
                labelMaxFileSize="최대 파일 크기: {filesize}"
                labelMaxFileSizeExceeded="파일 크기 초과"
                labelFileProcessingError="업로드 실패"
                credits={false}
                beforeAddFile={() => {
                  const total =
                    editExistingFiles.length +
                    stableFilesRef.current.length +
                    approvedCountRef.current
                  if (total >= boardMaster!.fileMaxCount) {
                    if (!alertShownRef.current) {
                      alertShownRef.current = true
                      const prevStable = stableFilesRef.current
                      setTimeout(() => {
                        alert(
                          `첨부파일은 최대 ${boardMaster!.fileMaxCount}개까지 등록할 수 있습니다.`,
                        )
                        const stableIds = new Set(prevStable.map((f) => f.id))
                        ;(pondRef.current?.getFiles() ?? [])
                          .filter((f) => !stableIds.has(f.id))
                          .forEach((f) => pondRef.current?.removeFile(f))
                        approvedCountRef.current = 0
                        alertShownRef.current = false
                      }, 0)
                    }
                    return false
                  }
                  approvedCountRef.current++
                  return true
                }}
              />
            </div>
          )}

          {error && (
            <p className="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
              {error}
            </p>
          )}
          <div className="flex justify-end gap-2 pt-2 border-t border-ink-100">
            <button
              type="button"
              onClick={() =>
                navigate(
                  isEdit ? `/board/${boardMasterId}/post/${postId}` : `/board/${boardMasterId}`,
                )
              }
              className="btn-secondary"
            >
              취소
            </button>
            <button disabled={loading} className="btn-primary">
              {loading ? (isEdit ? '수정 중…' : '등록 중…') : isEdit ? '수정' : '등록'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
