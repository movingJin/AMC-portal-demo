import { useEffect, useRef, useState } from 'react'
import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import { TextStyle, FontFamily, FontSize, LineHeight, Color } from '@tiptap/extension-text-style'
import Highlight from '@tiptap/extension-highlight'
import TextAlign from '@tiptap/extension-text-align'
import Link from '@tiptap/extension-link'
import Image from '@tiptap/extension-image'
import Youtube from '@tiptap/extension-youtube'
import { Table, TableRow, TableCell, TableHeader } from '@tiptap/extension-table'
import TaskList from '@tiptap/extension-task-list'
import TaskItem from '@tiptap/extension-task-item'
import CharacterCount from '@tiptap/extension-character-count'
import Placeholder from '@tiptap/extension-placeholder'
import './RichTextEditor.css'

// ── 상수 ────────────────────────────────────────────
const FONT_FAMILIES = [
  { label: '맑은 고딕', value: '맑은 고딕, sans-serif' },
  { label: '굴림',      value: '굴림, sans-serif' },
  { label: '바탕',      value: '바탕, serif' },
  { label: '나눔고딕',  value: '나눔고딕, sans-serif' },
  { label: '고정폭',    value: 'Courier New, monospace' },
]
const FONT_SIZES   = ['10', '12', '14', '16', '18', '20', '24', '28', '32', '36', '48']
const LINE_HEIGHTS = [
  { label: '1.0', value: '1' }, { label: '1.2', value: '1.2' },
  { label: '1.5', value: '1.5' }, { label: '1.75', value: '1.75' },
  { label: '2.0', value: '2' },
]
const QUICK_EMOJIS = [
  '😀','😂','😍','😊','😎','🤔','😭','🙏',
  '👍','👎','👏','💪','👋','👉','👀',
  '❤️','🔥','✨','⭐','💯',
  '✅','❌','⚠️','💡','🚀','🎉','💻','📝',
]
const HEADINGS = [1, 2, 3] as const

// ── 공통 컴포넌트 ────────────────────────────────────
function Sep() {
  return <span className="w-px h-5 bg-ink-200 mx-0.5 shrink-0 self-center" />
}

function Btn({ title, active, disabled, onClick, children }: {
  title: string; active?: boolean; disabled?: boolean
  onClick: () => void; children: React.ReactNode
}) {
  return (
    <button
      type="button"
      title={title}
      disabled={disabled}
      onMouseDown={(e) => { e.preventDefault(); onClick() }}
      className={[
        'inline-flex items-center justify-center min-w-[26px] h-[26px] px-1 rounded transition select-none text-xs',
        active   ? 'bg-brand-100 text-brand-700' : 'text-ink-600 hover:bg-ink-100 hover:text-ink-900',
        disabled ? 'opacity-30 cursor-not-allowed pointer-events-none' : '',
      ].join(' ')}
    >
      {children}
    </button>
  )
}

const SEL = 'h-[26px] text-[11px] border border-ink-200 rounded px-1.5 bg-white text-ink-700 focus:outline-none focus:ring-1 focus:ring-brand-300'

// ── SVG 아이콘 ──────────────────────────────────────
const IcoUndo    = () => <svg viewBox="0 0 24 24" className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M3 7v6h6"/><path d="M21 17A9 9 0 0 0 3 13"/></svg>
const IcoRedo    = () => <svg viewBox="0 0 24 24" className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M21 7v6h-6"/><path d="M3 17a9 9 0 0 1 18-4"/></svg>
const IcoBullet  = () => <svg viewBox="0 0 24 24" className="w-3.5 h-3.5" fill="currentColor"><circle cx="4" cy="6" r="1.5"/><circle cx="4" cy="12" r="1.5"/><circle cx="4" cy="18" r="1.5"/><rect x="7" y="5" width="13" height="2"/><rect x="7" y="11" width="13" height="2"/><rect x="7" y="17" width="13" height="2"/></svg>
const IcoOrdered = () => <svg viewBox="0 0 24 24" className="w-3.5 h-3.5" fill="currentColor"><path d="M2 17h2v.5H3v1h1v.5H2v1h3v-4H2v1zm1-9h1V4H2v1h1v3zm-1 3h1.8L2 13.1v.9h3v-1H3.2L5 10.9V10H2v1zm5-6v2h14V5H7zm0 14h14v-2H7v2zm0-6h14v-2H7v2z"/></svg>
const IcoTask    = () => <svg viewBox="0 0 24 24" className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="5" width="5" height="5" rx="1"/><path d="m4 7.5 1.5 1.5L8 6"/><rect x="3" y="13" width="5" height="5" rx="1"/><line x1="14" y1="7" x2="21" y2="7"/><line x1="14" y1="15" x2="21" y2="15"/></svg>
const IcoAlignL  = () => <svg viewBox="0 0 24 24" className="w-3.5 h-3.5" fill="currentColor"><rect x="3" y="5" width="12" height="2"/><rect x="3" y="9" width="18" height="2"/><rect x="3" y="13" width="12" height="2"/><rect x="3" y="17" width="18" height="2"/></svg>
const IcoAlignC  = () => <svg viewBox="0 0 24 24" className="w-3.5 h-3.5" fill="currentColor"><rect x="6" y="5" width="12" height="2"/><rect x="3" y="9" width="18" height="2"/><rect x="6" y="13" width="12" height="2"/><rect x="3" y="17" width="18" height="2"/></svg>
const IcoAlignR  = () => <svg viewBox="0 0 24 24" className="w-3.5 h-3.5" fill="currentColor"><rect x="9" y="5" width="12" height="2"/><rect x="3" y="9" width="18" height="2"/><rect x="9" y="13" width="12" height="2"/><rect x="3" y="17" width="18" height="2"/></svg>
const IcoAlignJ  = () => <svg viewBox="0 0 24 24" className="w-3.5 h-3.5" fill="currentColor"><rect x="3" y="5" width="18" height="2"/><rect x="3" y="9" width="18" height="2"/><rect x="3" y="13" width="18" height="2"/><rect x="3" y="17" width="18" height="2"/></svg>
const IcoQuote   = () => <svg viewBox="0 0 24 24" className="w-3.5 h-3.5" fill="currentColor"><path d="M3 21c3 0 7-1 7-8V5c0-1.25-.756-2.017-2-2H4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V20c0 1 0 1 1 1z"/><path d="M15 21c3 0 7-1 7-8V5c0-1.25-.757-2.017-2-2h-4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2h.75c0 2.25.25 4-2.75 4v3c0 1 0 1 1 1z"/></svg>
const IcoCode    = () => <svg viewBox="0 0 24 24" className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>
const IcoHR      = () => <svg viewBox="0 0 24 24" className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2"><line x1="3" y1="12" x2="21" y2="12"/></svg>
const IcoLink    = () => <svg viewBox="0 0 24 24" className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
const IcoUnlink  = () => <svg viewBox="0 0 24 24" className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/><line x1="2" y1="2" x2="22" y2="22"/></svg>
const IcoImage   = () => <svg viewBox="0 0 24 24" className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
const IcoYT      = () => <svg viewBox="0 0 24 24" className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M22.54 6.42a2.78 2.78 0 0 0-1.95-1.96C18.88 4 12 4 12 4s-6.88 0-8.59.46a2.78 2.78 0 0 0-1.95 1.96A29 29 0 0 0 1 12a29 29 0 0 0 .46 5.58A2.78 2.78 0 0 0 3.41 19.6C5.12 20 12 20 12 20s6.88 0 8.59-.46a2.78 2.78 0 0 0 1.95-1.95A29 29 0 0 0 23 12a29 29 0 0 0-.46-5.58z"/><polygon points="9.75 15.02 15.5 12 9.75 8.98 9.75 15.02" fill="currentColor" stroke="none"/></svg>
const IcoTable   = () => <svg viewBox="0 0 24 24" className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="3" width="18" height="18" rx="1"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="3" y1="15" x2="21" y2="15"/><line x1="9" y1="3" x2="9" y2="21"/><line x1="15" y1="3" x2="15" y2="21"/></svg>

// ── 메인 컴포넌트 ────────────────────────────────────
interface Props { value: string; onChange: (html: string) => void }

export default function RichTextEditor({ value, onChange }: Props) {
  const colorRef = useRef<HTMLInputElement>(null)
  const hlRef    = useRef<HTMLInputElement>(null)
  const emojiRef = useRef<HTMLDivElement>(null)
  const [emojiOpen, setEmojiOpen] = useState(false)

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (emojiRef.current && !emojiRef.current.contains(e.target as Node))
        setEmojiOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const editor = useEditor({
    extensions: [
      StarterKit,
      Underline,
      TextStyle,
      FontFamily,
      FontSize,
      LineHeight,
      Color,
      Highlight.configure({ multicolor: true }),
      TextAlign.configure({ types: ['heading', 'paragraph'] }),
      Link.configure({ openOnClick: false }),
      Image,
      Youtube.configure({ controls: true }),
      Table.configure({ resizable: true }),
      TableRow,
      TableCell,
      TableHeader,
      TaskList,
      TaskItem.configure({ nested: true }),
      CharacterCount,
      Placeholder.configure({ placeholder: '내용을 입력하세요.' }),
    ],
    content: value,
    onUpdate: ({ editor }) => onChange(editor.getHTML()),
  })

  useEffect(() => {
    if (editor && value !== editor.getHTML() &&
        !document.activeElement?.closest('.ProseMirror')) {
      editor.commands.setContent(value, { emitUpdate: false })
    }
  }, [value, editor])

  if (!editor) return null

  const inTable   = editor.isActive('table')
  const tsAttrs   = editor.getAttributes('textStyle') as {
    fontFamily?: string; fontSize?: string; lineHeight?: string
  }

  const insertLink = () => {
    const prev = editor.getAttributes('link').href as string | undefined
    const url  = prompt('링크 URL을 입력하세요.', prev ?? 'https://')
    if (url === null) return
    if (!url.trim()) editor.chain().focus().unsetLink().run()
    else             editor.chain().focus().setLink({ href: url }).run()
  }
  const insertImage   = () => { const u = prompt('이미지 URL을 입력하세요.'); if (u) editor.chain().focus().setImage({ src: u }).run() }
  const insertYoutube = () => { const u = prompt('YouTube URL을 입력하세요.'); if (u) editor.chain().focus().setYoutubeVideo({ src: u }).run() }
  const insertTable   = () => editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()

  return (
    <div className="border border-ink-200 rounded-lg">

      {/* ── 툴바 ── */}
      <div className="flex flex-wrap items-center gap-0.5 px-2 py-1.5 bg-ink-50 border-b border-ink-200 sticky top-14 z-20 rounded-t-lg">

        {/* 글꼴 */}
        <select value={tsAttrs.fontFamily ?? ''} className={`${SEL} min-w-[100px]`}
          onChange={(e) => {
            const v = e.target.value
            if (!v) editor.chain().focus().unsetFontFamily().run()
            else    editor.chain().focus().setFontFamily(v).run()
          }}>
          <option value="">기본 글꼴</option>
          {FONT_FAMILIES.map(f => <option key={f.value} value={f.value}>{f.label}</option>)}
        </select>

        {/* 글자 크기 */}
        <select value={tsAttrs.fontSize?.replace('px', '') ?? ''} className={`${SEL} min-w-[76px]`}
          onChange={(e) => {
            const v = e.target.value
            if (!v) editor.chain().focus().unsetFontSize().run()
            else    editor.chain().focus().setFontSize(`${v}px`).run()
          }}>
          <option value="">기본 크기</option>
          {FONT_SIZES.map(s => <option key={s} value={s}>{s}px</option>)}
        </select>

        {/* 줄 간격 */}
        <select value={tsAttrs.lineHeight ?? ''} className={`${SEL} min-w-[80px]`}
          onChange={(e) => {
            const v = e.target.value
            if (!v) editor.chain().focus().unsetLineHeight().run()
            else    editor.chain().focus().setLineHeight(v).run()
          }}>
          <option value="">기본 간격</option>
          {LINE_HEIGHTS.map(l => <option key={l.value} value={l.value}>{l.label}</option>)}
        </select>

        <Sep />

        {/* Undo / Redo */}
        <Btn title="실행 취소 (Ctrl+Z)" disabled={!editor.can().undo()} onClick={() => editor.chain().focus().undo().run()}><IcoUndo /></Btn>
        <Btn title="다시 실행 (Ctrl+Y)" disabled={!editor.can().redo()} onClick={() => editor.chain().focus().redo().run()}><IcoRedo /></Btn>

        <Sep />

        {/* 텍스트 서식 */}
        <Btn title="굵게 (Ctrl+B)"   active={editor.isActive('bold')}      onClick={() => editor.chain().focus().toggleBold().run()}>      <strong className="font-bold">B</strong></Btn>
        <Btn title="기울임 (Ctrl+I)" active={editor.isActive('italic')}    onClick={() => editor.chain().focus().toggleItalic().run()}>    <em className="italic">I</em></Btn>
        <Btn title="밑줄 (Ctrl+U)"   active={editor.isActive('underline')} onClick={() => editor.chain().focus().toggleUnderline().run()}> <span className="underline">U</span></Btn>
        <Btn title="취소선"           active={editor.isActive('strike')}    onClick={() => editor.chain().focus().toggleStrike().run()}>    <span className="line-through">S</span></Btn>
        <Btn title="인라인 코드"      active={editor.isActive('code')}      onClick={() => editor.chain().focus().toggleCode().run()}>      <code className="font-mono text-[10px]">&lt;/&gt;</code></Btn>

        <Sep />

        {/* 제목 */}
        {HEADINGS.map(level => (
          <Btn key={level} title={`제목 ${level}`} active={editor.isActive('heading', { level })}
            onClick={() => editor.chain().focus().toggleHeading({ level }).run()}>
            <span className="font-bold">H{level}</span>
          </Btn>
        ))}

        <Sep />

        {/* 텍스트 색상 */}
        <Btn title="텍스트 색상" onClick={() => colorRef.current?.click()}>
          <span className="font-bold" style={{ color: (editor.getAttributes('textStyle').color as string) ?? '#111' }}>A</span>
        </Btn>
        <input ref={colorRef} type="color" defaultValue="#000000" className="hidden"
          onInput={(e) => editor.chain().focus().setColor((e.target as HTMLInputElement).value).run()} />

        {/* 형광펜 */}
        <Btn title="형광펜" active={editor.isActive('highlight')} onClick={() => hlRef.current?.click()}>
          <span className="font-bold px-0.5" style={{ background: '#fef08a' }}>A</span>
        </Btn>
        <input ref={hlRef} type="color" defaultValue="#ffff00" className="hidden"
          onInput={(e) => editor.chain().focus().toggleHighlight({ color: (e.target as HTMLInputElement).value }).run()} />

        <Sep />

        {/* 목록 */}
        <Btn title="글머리 기호" active={editor.isActive('bulletList')}  onClick={() => editor.chain().focus().toggleBulletList().run()}>  <IcoBullet /></Btn>
        <Btn title="번호 목록"   active={editor.isActive('orderedList')} onClick={() => editor.chain().focus().toggleOrderedList().run()}> <IcoOrdered /></Btn>
        <Btn title="할일 목록"   active={editor.isActive('taskList')}    onClick={() => editor.chain().focus().toggleTaskList().run()}>    <IcoTask /></Btn>

        <Sep />

        {/* 정렬 */}
        <Btn title="왼쪽 정렬"   active={editor.isActive({ textAlign: 'left' })}    onClick={() => editor.chain().focus().setTextAlign('left').run()}>    <IcoAlignL /></Btn>
        <Btn title="가운데 정렬" active={editor.isActive({ textAlign: 'center' })}  onClick={() => editor.chain().focus().setTextAlign('center').run()}>  <IcoAlignC /></Btn>
        <Btn title="오른쪽 정렬" active={editor.isActive({ textAlign: 'right' })}   onClick={() => editor.chain().focus().setTextAlign('right').run()}>   <IcoAlignR /></Btn>
        <Btn title="양쪽 정렬"   active={editor.isActive({ textAlign: 'justify' })} onClick={() => editor.chain().focus().setTextAlign('justify').run()}> <IcoAlignJ /></Btn>

        <Sep />

        {/* 블록 */}
        <Btn title="인용문"    active={editor.isActive('blockquote')} onClick={() => editor.chain().focus().toggleBlockquote().run()}>  <IcoQuote /></Btn>
        <Btn title="코드 블록" active={editor.isActive('codeBlock')}  onClick={() => editor.chain().focus().toggleCodeBlock().run()}>   <IcoCode /></Btn>
        <Btn title="구분선"                                            onClick={() => editor.chain().focus().setHorizontalRule().run()}> <IcoHR /></Btn>

        <Sep />

        {/* 링크 / 미디어 */}
        <Btn title="링크 삽입" active={editor.isActive('link')}    onClick={insertLink}>                                              <IcoLink /></Btn>
        <Btn title="링크 제거" disabled={!editor.isActive('link')} onClick={() => editor.chain().focus().unsetLink().run()}>           <IcoUnlink /></Btn>
        <Btn title="이미지 삽입"                                     onClick={insertImage}>                                             <IcoImage /></Btn>
        <Btn title="YouTube 삽입"                                    onClick={insertYoutube}>                                           <IcoYT /></Btn>

        {/* 이모지 */}
        <div className="relative" ref={emojiRef}>
          <Btn title="이모지" active={emojiOpen} onClick={() => setEmojiOpen(v => !v)}>
            <span className="text-sm">😊</span>
          </Btn>
          {emojiOpen && (
            <div className="absolute top-full left-0 z-50 mt-1 p-2 bg-white border border-ink-200 rounded-lg shadow-lg w-52">
              <div className="grid grid-cols-7 gap-0.5">
                {QUICK_EMOJIS.map((e) => (
                  <button key={e} type="button"
                    className="text-base w-7 h-7 flex items-center justify-center rounded hover:bg-ink-100 transition"
                    onMouseDown={(ev) => {
                      ev.preventDefault()
                      editor.chain().focus().insertContent(e).run()
                      setEmojiOpen(false)
                    }}
                  >{e}</button>
                ))}
              </div>
              <p className="text-[10px] text-ink-400 mt-1.5 px-0.5">또는 편집기에서 이모지를 직접 입력</p>
            </div>
          )}
        </div>

        <Sep />

        {/* 표 */}
        <Btn title="표 삽입" onClick={insertTable}>                                                                           <IcoTable /></Btn>
        <Btn title="행 추가" disabled={!inTable} onClick={() => editor.chain().focus().addRowAfter().run()}>    <span className="text-[10px] font-medium">행+</span></Btn>
        <Btn title="열 추가" disabled={!inTable} onClick={() => editor.chain().focus().addColumnAfter().run()}> <span className="text-[10px] font-medium">열+</span></Btn>
        <Btn title="행 삭제" disabled={!inTable} onClick={() => editor.chain().focus().deleteRow().run()}>      <span className="text-[10px] font-medium text-red-500">행-</span></Btn>
        <Btn title="열 삭제" disabled={!inTable} onClick={() => editor.chain().focus().deleteColumn().run()}>   <span className="text-[10px] font-medium text-red-500">열-</span></Btn>
        <Btn title="표 삭제" disabled={!inTable} onClick={() => editor.chain().focus().deleteTable().run()}>    <span className="text-[10px] font-medium text-red-500">표✕</span></Btn>
      </div>

      {/* ── 편집 영역 ── */}
      <div className="editor-content p-4">
        <EditorContent editor={editor} />
      </div>

      {/* ── 글자 수 ── */}
      <div className="border-t border-ink-200 bg-ink-50 px-3 py-1 flex justify-end rounded-b-lg">
        <span className="text-xs text-ink-400">
          {editor.storage.characterCount.characters().toLocaleString()} 자
        </span>
      </div>
    </div>
  )
}
