import { useEffect, useState } from 'react'
import { api, ApiError } from '@/lib/api'
import { useAuth } from '@/lib/auth'
import UserSearchModal from '@/components/UserSearchModal'
import MemberHistoryModal from './MemberHistoryModal'

type Project = {
  id: number
  name: string
  description: string | null
  createdByName: string
  createdAt: string
}

type ProjectMember = {
  id: number
  userId: number
  displayName: string
  email: string
  role: 'ADMIN' | 'CONTRIBUTOR' | 'VIEWER'
  joinedAt: string
}

const ROLE_BADGE: Record<string, string> = {
  ADMIN: 'bg-brand-100 text-brand-700',
  CONTRIBUTOR: 'bg-emerald-50 text-emerald-700',
  VIEWER: 'bg-ink-100 text-ink-600',
}

const ROLE_LABEL: Record<string, string> = {
  ADMIN: '관리자',
  CONTRIBUTOR: '기여자',
  VIEWER: '뷰어',
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString('ko-KR', {
    year: '2-digit',
    month: '2-digit',
    day: '2-digit',
  })
}

export default function ProjectListPage() {
  const user = useAuth((s) => s.user)
  const [projects, setProjects] = useState<Project[] | null>(null)
  const [selected, setSelected] = useState<Project | null>(null)
  const [members, setMembers] = useState<ProjectMember[] | null>(null)
  const [membersLoading, setMembersLoading] = useState(false)
  const [search, setSearch] = useState('')
  const [adding, setAdding] = useState(false)
  const [newName, setNewName] = useState('')
  const [saving, setSaving] = useState(false)
  const [checkedIds, setCheckedIds] = useState<Set<number>>(new Set())
  const [roleUpdating, setRoleUpdating] = useState<Set<number>>(new Set())
  const [pendingRole, setPendingRole] = useState<{ member: ProjectMember; role: string } | null>(
    null,
  )
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [editing, setEditing] = useState(false)
  const [editName, setEditName] = useState('')
  const [editDesc, setEditDesc] = useState('')
  const [editSaving, setEditSaving] = useState(false)
  const [editError, setEditError] = useState<string | null>(null)
  const [alertMessage, setAlertMessage] = useState<string | null>(null)
  const [memberModalOpen, setMemberModalOpen] = useState(false)
  const [historyModalOpen, setHistoryModalOpen] = useState(false)

  const isAdmin = members?.some((m) => m.userId === user?.id && m.role === 'ADMIN') ?? false

  const filtered =
    projects?.filter((p) => p.name.toLowerCase().includes(search.toLowerCase())) ?? null

  useEffect(() => {
    api<Project[]>('/api/projects')
      .then(setProjects)
      .catch(() => setProjects([]))
  }, [])

  const handleAdd = () => {
    setAdding(true)
    setNewName('')
  }

  const handleCancelAdd = () => setAdding(false)

  const handleSaveAdd = async () => {
    if (!newName.trim()) return
    setSaving(true)
    try {
      const created = await api<Project>('/api/projects', {
        method: 'POST',
        body: JSON.stringify({ name: newName.trim() }),
      })
      setProjects((prev) => (prev ? [created, ...prev] : [created]))
      setAdding(false)
      handleSelectProject(created)
    } finally {
      setSaving(false)
    }
  }

  const handleSelectProject = (project: Project) => {
    if (selected?.id === project.id) return
    setSelected(project)
    setMembers(null)
    setCheckedIds(new Set())
    setEditing(false)
    setMembersLoading(true)
    api<ProjectMember[]>(`/api/projects/${project.id}/members`)
      .then(setMembers)
      .catch(() => setMembers([]))
      .finally(() => setMembersLoading(false))
  }

  const handleStartEdit = () => {
    if (!selected) return
    setEditName(selected.name)
    setEditDesc(selected.description ?? '')
    setEditing(true)
  }

  const handleCancelEdit = () => setEditing(false)

  const handleSaveEdit = async () => {
    if (!selected || !editName.trim()) return
    if (editName.trim().length > 100) {
      setEditError(
        `프로젝트명은 최대 100자까지 입력할 수 있습니다. (현재 ${editName.trim().length}자)`,
      )
      return
    }
    if (editDesc.trim().length > 500) {
      setEditError(
        `프로젝트 설명은 최대 500자까지 입력할 수 있습니다. (현재 ${editDesc.trim().length}자)`,
      )
      return
    }
    setEditSaving(true)
    try {
      const updated = await api<Project>(`/api/projects/${selected.id}`, {
        method: 'PATCH',
        body: JSON.stringify({ name: editName.trim(), description: editDesc.trim() || null }),
      })
      setSelected(updated)
      setProjects((prev) => prev?.map((p) => (p.id === updated.id ? updated : p)) ?? prev)
      setEditing(false)
    } finally {
      setEditSaving(false)
    }
  }

  const toggleCheck = (id: number) => {
    setCheckedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  const handleDeleteMembers = async () => {
    if (!selected || checkedIds.size === 0) return
    setDeleting(true)
    try {
      const updated = await api<ProjectMember[]>(`/api/projects/${selected.id}/members`, {
        method: 'DELETE',
        body: JSON.stringify({ memberIds: Array.from(checkedIds) }),
      })
      setMembers(updated)
      setCheckedIds(new Set())
      setDeleteConfirmOpen(false)
    } catch (e) {
      setDeleteConfirmOpen(false)
      setAlertMessage(e instanceof ApiError ? e.message : '멤버 삭제 중 오류가 발생했습니다.')
    } finally {
      setDeleting(false)
    }
  }

  const handleRoleChange = async (member: ProjectMember, role: string) => {
    if (!selected) return
    setRoleUpdating((prev) => new Set(prev).add(member.id))
    try {
      const updated = await api<ProjectMember>(
        `/api/projects/${selected.id}/members/${member.id}/role`,
        {
          method: 'PATCH',
          body: JSON.stringify({ role }),
        },
      )
      setMembers((prev) => prev?.map((m) => (m.id === updated.id ? updated : m)) ?? prev)
    } catch (e) {
      setAlertMessage(e instanceof ApiError ? e.message : '역할 변경 중 오류가 발생했습니다.')
    } finally {
      setRoleUpdating((prev) => {
        const s = new Set(prev)
        s.delete(member.id)
        return s
      })
    }
  }

  const allChecked = (members?.length ?? 0) > 0 && members!.every((m) => checkedIds.has(m.id))
  const toggleAll = () => {
    if (allChecked) setCheckedIds(new Set())
    else setCheckedIds(new Set(members?.map((m) => m.id)))
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <header>
        <h1 className="text-2xl font-semibold tracking-tight">프로젝트 관리</h1>
        <p className="text-sm text-ink-500 mt-1">
          {projects ? `전체 ${projects.length.toLocaleString()}개의 프로젝트` : '불러오는 중…'}
        </p>
      </header>

      <div className="grid grid-cols-2 gap-4 items-start">
        {/* 프로젝트 목록 */}
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <div className="relative flex-1">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-ink-400"
              >
                <circle cx="11" cy="11" r="7" stroke="currentColor" strokeWidth="1.8" />
                <path
                  d="m20 20-3.5-3.5"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                />
              </svg>
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="프로젝트명으로 검색"
                className="input pl-9"
              />
            </div>
            {user && (
              <button onClick={handleAdd} className="btn-primary shrink-0">
                <svg viewBox="0 0 24 24" fill="none" className="w-4 h-4">
                  <path
                    d="M12 5v14M5 12h14"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                  />
                </svg>
                행추가
              </button>
            )}
          </div>
          <div className="card overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs uppercase tracking-wider text-ink-500 bg-ink-50/60">
                  <th className="px-5 py-3 font-medium">프로젝트명</th>
                  <th className="px-5 py-3 w-36 font-medium">생성자</th>
                  <th className="px-5 py-3 w-32 font-medium">생성일</th>
                </tr>
              </thead>
              <tbody>
                {adding && (
                  <tr className="border-t border-ink-100 bg-brand-50/40">
                    <td className="px-5 py-2.5">
                      <input
                        autoFocus
                        value={newName}
                        onChange={(e) => setNewName(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter') handleSaveAdd()
                          if (e.key === 'Escape') handleCancelAdd()
                        }}
                        placeholder="프로젝트명"
                        className="input py-1 text-sm w-full"
                      />
                    </td>
                    <td className="px-5 py-2.5 text-ink-600 text-sm">{user?.displayName}</td>
                    <td className="px-5 py-2.5">
                      <div className="flex gap-1">
                        <button
                          onClick={handleSaveAdd}
                          disabled={!newName.trim() || saving}
                          title="확인"
                          className="grid place-items-center w-7 h-7 rounded-md bg-emerald-400 text-white hover:bg-emerald-500 disabled:opacity-40 transition"
                        >
                          <svg viewBox="0 0 24 24" fill="none" className="w-4 h-4">
                            <path
                              d="M5 13l4 4L19 7"
                              stroke="currentColor"
                              strokeWidth="2"
                              strokeLinecap="round"
                              strokeLinejoin="round"
                            />
                          </svg>
                        </button>
                        <button
                          onClick={handleCancelAdd}
                          title="취소"
                          className="grid place-items-center w-7 h-7 rounded-md bg-red-400 text-white hover:bg-red-500 transition"
                        >
                          <svg viewBox="0 0 24 24" fill="none" className="w-4 h-4">
                            <path
                              d="M18 6 6 18M6 6l12 12"
                              stroke="currentColor"
                              strokeWidth="2"
                              strokeLinecap="round"
                            />
                          </svg>
                        </button>
                      </div>
                    </td>
                  </tr>
                )}
                {!projects &&
                  [...Array(6)].map((_, i) => (
                    <tr key={i} className="border-t border-ink-100">
                      <td className="px-5 py-3.5">
                        <div className="skeleton h-3 w-3/4" />
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="skeleton h-3 w-20" />
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="skeleton h-3 w-16" />
                      </td>
                    </tr>
                  ))}
                {filtered?.map((project) => (
                  <tr
                    key={project.id}
                    onClick={() => handleSelectProject(project)}
                    className={`border-t border-ink-100 transition cursor-pointer ${
                      selected?.id === project.id ? 'bg-brand-50' : 'hover:bg-ink-50/60'
                    }`}
                  >
                    <td className="px-5 py-3.5 max-w-0">
                      <div title={project.name} className="font-medium text-ink-800 truncate">
                        {project.name}
                      </div>
                    </td>
                    <td className="px-5 py-3.5 text-ink-600">{project.createdByName}</td>
                    <td className="px-5 py-3.5 text-ink-500">{formatDate(project.createdAt)}</td>
                  </tr>
                ))}
                {filtered?.length === 0 && (
                  <tr>
                    <td colSpan={3} className="text-center py-16 text-ink-400">
                      <div className="text-3xl mb-2">∅</div>
                      {search ? '검색 결과가 없습니다.' : '프로젝트가 없습니다.'}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* 멤버 목록 */}
        <div className="card overflow-hidden">
          {!selected ? (
            <div className="py-16 text-center text-ink-400 text-sm">
              왼쪽에서 프로젝트를 선택하세요.
            </div>
          ) : (
            <>
              <div className="px-5 py-4 border-b border-ink-100 bg-ink-50/60 space-y-2">
                {editing ? (
                  <>
                    <div>
                      <input
                        value={editName}
                        onChange={(e) => setEditName(e.target.value)}
                        placeholder="프로젝트명"
                        className={`input py-1 text-sm w-full font-semibold ${editName.trim().length > 100 ? 'border-red-400 focus:ring-red-400' : ''}`}
                      />
                      <p
                        className={`text-right text-xs mt-0.5 ${editName.trim().length > 100 ? 'text-red-500' : 'text-ink-400'}`}
                      >
                        {editName.trim().length}/100
                      </p>
                    </div>
                    <div>
                      <textarea
                        value={editDesc}
                        onChange={(e) => setEditDesc(e.target.value)}
                        placeholder="프로젝트 설명 (선택)"
                        rows={3}
                        className={`input py-1 text-xs w-full resize-none ${editDesc.trim().length > 500 ? 'border-red-400 focus:ring-red-400' : ''}`}
                      />
                      <p
                        className={`text-right text-xs mt-0.5 ${editDesc.trim().length > 500 ? 'text-red-500' : 'text-ink-400'}`}
                      >
                        {editDesc.trim().length}/500
                      </p>
                    </div>
                    <div className="flex justify-end gap-1.5 pt-1">
                      <button onClick={handleCancelEdit} className="btn-secondary py-1 text-xs">
                        취소
                      </button>
                      <button
                        onClick={handleSaveEdit}
                        disabled={!editName.trim() || editSaving}
                        className="btn-primary py-1 text-xs disabled:opacity-40"
                      >
                        저장
                      </button>
                    </div>
                  </>
                ) : (
                  <>
                    <p className="text-sm font-semibold text-ink-800 break-words">
                      {selected.name}
                    </p>
                    {selected.description && (
                      <p className="text-xs text-ink-500">{selected.description}</p>
                    )}
                    <div className="flex flex-wrap justify-end gap-1.5 pt-3">
                      {isAdmin && (
                        <button onClick={handleStartEdit} className="btn-secondary py-1 text-xs">
                          프로젝트 수정
                        </button>
                      )}
                      <button
                        onClick={() => setHistoryModalOpen(true)}
                        className="btn-secondary py-1 text-xs"
                      >
                        멤버 이력
                      </button>
                      {isAdmin && (
                        <>
                          <button
                            onClick={() => checkedIds.size > 0 && setDeleteConfirmOpen(true)}
                            disabled={checkedIds.size === 0}
                            className="btn-danger py-1 text-xs disabled:opacity-40"
                          >
                            멤버 삭제
                          </button>
                          <button
                            onClick={() => setMemberModalOpen(true)}
                            className="btn-primary py-1 text-xs"
                          >
                            멤버 추가
                          </button>
                        </>
                      )}
                    </div>
                  </>
                )}
              </div>
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-xs uppercase tracking-wider text-ink-500 bg-ink-50/40">
                    <th className="px-4 py-3 w-8">
                      <input
                        type="checkbox"
                        checked={allChecked}
                        onChange={toggleAll}
                        disabled={!isAdmin}
                        className="rounded border-ink-300 text-brand-600 focus:ring-brand-500 disabled:opacity-30"
                      />
                    </th>
                    <th className="px-5 py-3 font-medium">이름</th>
                    <th className="px-5 py-3 font-medium">이메일</th>
                    <th className="px-5 py-3 w-24 font-medium">역할</th>
                  </tr>
                </thead>
                <tbody>
                  {membersLoading &&
                    [...Array(4)].map((_, i) => (
                      <tr key={i} className="border-t border-ink-100">
                        <td className="px-4 py-3.5">
                          <div className="skeleton h-3.5 w-3.5 rounded" />
                        </td>
                        <td className="px-5 py-3.5">
                          <div className="skeleton h-3 w-20" />
                        </td>
                        <td className="px-5 py-3.5">
                          <div className="skeleton h-3 w-32" />
                        </td>
                        <td className="px-5 py-3.5">
                          <div className="skeleton h-4 w-14 rounded-full" />
                        </td>
                      </tr>
                    ))}
                  {members?.map((member) => (
                    <tr
                      key={member.id}
                      onClick={() => isAdmin && toggleCheck(member.id)}
                      className={`border-t border-ink-100 transition ${isAdmin ? 'cursor-pointer' : 'cursor-default'} ${checkedIds.has(member.id) ? 'bg-brand-50' : 'hover:bg-ink-50/60'}`}
                    >
                      <td className="px-4 py-3.5" onClick={(e) => e.stopPropagation()}>
                        <input
                          type="checkbox"
                          checked={checkedIds.has(member.id)}
                          disabled={!isAdmin}
                          onChange={() => toggleCheck(member.id)}
                          className="rounded border-ink-300 text-brand-600 focus:ring-brand-500 disabled:opacity-30"
                        />
                      </td>
                      <td className="px-5 py-3.5 font-medium text-ink-800">{member.displayName}</td>
                      <td className="px-5 py-3.5 text-ink-500 text-xs">{member.email}</td>
                      <td className="px-5 py-3.5" onClick={(e) => e.stopPropagation()}>
                        {isAdmin ? (
                          <select
                            value={member.role}
                            disabled={roleUpdating.has(member.id)}
                            onChange={(e) => setPendingRole({ member, role: e.target.value })}
                            className="text-xs border border-ink-200 rounded-md px-1.5 py-0.5 bg-white text-ink-700 focus:outline-none focus:ring-1 focus:ring-brand-500 disabled:opacity-40"
                          >
                            <option value="ADMIN">관리자</option>
                            <option value="CONTRIBUTOR">기여자</option>
                            <option value="VIEWER">뷰어</option>
                          </select>
                        ) : (
                          <span
                            className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${ROLE_BADGE[member.role]}`}
                          >
                            {ROLE_LABEL[member.role]}
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                  {members?.length === 0 && (
                    <tr>
                      <td colSpan={4} className="text-center py-12 text-ink-400">
                        멤버가 없습니다.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </>
          )}
        </div>
      </div>

      {selected && (
        <UserSearchModal
          title="멤버 추가"
          open={memberModalOpen}
          multiSelect
          excludeIds={members?.map((m) => m.userId)}
          onClose={() => setMemberModalOpen(false)}
          onConfirm={async (users) => {
            if (!users.length) return
            const updated = await api<ProjectMember[]>(`/api/projects/${selected.id}/members`, {
              method: 'POST',
              body: JSON.stringify({ userIds: users.map((u) => u.id) }),
            })
            setMembers(updated)
            setMemberModalOpen(false)
          }}
        />
      )}
      {selected && (
        <MemberHistoryModal
          projectId={selected.id}
          open={historyModalOpen}
          onClose={() => setHistoryModalOpen(false)}
        />
      )}
      {deleteConfirmOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white rounded-xl shadow-xl px-6 py-5 mx-4 space-y-4 w-72">
            <p className="text-sm text-ink-800 font-medium">
              선택한 <span className="font-semibold">{checkedIds.size}명</span>의 멤버를
              삭제하시겠습니까?
            </p>
            <div className="flex justify-center gap-2">
              <button
                onClick={() => setDeleteConfirmOpen(false)}
                disabled={deleting}
                className="btn-secondary py-1.5 text-xs"
              >
                취소
              </button>
              <button
                onClick={handleDeleteMembers}
                disabled={deleting}
                className="btn-danger py-1.5 text-xs disabled:opacity-40"
              >
                {deleting ? '삭제 중…' : '삭제'}
              </button>
            </div>
          </div>
        </div>
      )}
      {pendingRole && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white rounded-xl shadow-xl px-6 py-5 mx-4 space-y-4 w-72">
            <p className="text-sm text-ink-800 font-medium">
              <span className="font-semibold">{pendingRole.member.displayName}</span>님의 역할을{' '}
              <span className="font-semibold">{ROLE_LABEL[pendingRole.role]}</span>(으)로
              변경하시겠습니까?
            </p>
            <div className="flex justify-center gap-2">
              <button onClick={() => setPendingRole(null)} className="btn-secondary py-1.5 text-xs">
                취소
              </button>
              <button
                onClick={async () => {
                  const { member, role } = pendingRole
                  setPendingRole(null)
                  await handleRoleChange(member, role)
                }}
                className="btn-primary py-1.5 text-xs"
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}
      {editError && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white rounded-xl shadow-xl px-6 py-5 mx-4 space-y-4 w-80">
            <p className="text-sm text-ink-800 font-medium">{editError}</p>
            <div className="flex justify-center">
              <button onClick={() => setEditError(null)} className="btn-primary py-1.5 text-xs">
                확인
              </button>
            </div>
          </div>
        </div>
      )}
      {alertMessage && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white rounded-xl shadow-xl px-6 py-5 mx-4 space-y-4 w-80">
            <p className="text-sm text-ink-800 font-medium">{alertMessage}</p>
            <div className="flex justify-center">
              <button onClick={() => setAlertMessage(null)} className="btn-primary py-1.5 text-xs">
                확인
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
