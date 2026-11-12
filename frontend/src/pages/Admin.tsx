import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
  apiCreateMeetingType,
  apiDeleteMeetingType,
  apiListBookings,
  apiListMeetingTypes,
  apiUpdateMeetingType,
} from '../api/generated';
import type { Booking, MeetingType } from '../api/generated';
import { useGuest } from '../context/GuestContext';

type MeetingTypeForm = {
  name: string;
  description: string;
  durationMinutes: string;
};

const emptyForm: MeetingTypeForm = {
  name: '',
  description: '',
  durationMinutes: '30',
};

export default function AdminPage() {
  const { guest } = useGuest();
  const [meetingTypes, setMeetingTypes] = useState<MeetingType[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [form, setForm] = useState<MeetingTypeForm>(emptyForm);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const loadMeetingTypes = useCallback(async () => {
    const result = await apiListMeetingTypes();
    setMeetingTypes(result.data ?? []);
  }, []);

  const loadBookings = useCallback(async () => {
    const result = await apiListBookings();
    setBookings(result.data ?? []);
  }, []);

  useEffect(() => {
    void loadMeetingTypes();
    void loadBookings();
  }, [loadMeetingTypes, loadBookings]);

  const resetForm = () => {
    setForm(emptyForm);
    setEditingId(null);
    setFormError(null);
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const durationMinutes = Number(form.durationMinutes);
    if (!form.name.trim() || !form.description.trim() || !Number.isFinite(durationMinutes)) {
      setFormError('Заполните название, описание и длительность.');
      return;
    }

    setSaving(true);
    setFormError(null);
    const body = {
      name: form.name.trim(),
      description: form.description.trim(),
      durationMinutes,
    };
    const result = editingId
      ? await apiUpdateMeetingType({ path: { id: editingId }, body })
      : await apiCreateMeetingType({ body });
    setSaving(false);

    if (result.error) {
      setFormError('Не удалось сохранить тип встречи. Проверьте поля.');
      return;
    }

    resetForm();
    await loadMeetingTypes();
  };

  const startEdit = (type: MeetingType) => {
    setEditingId(type.id);
    setForm({
      name: type.name,
      description: type.description,
      durationMinutes: String(type.durationMinutes),
    });
    setFormError(null);
  };

  const remove = async (id: string) => {
    const result = await apiDeleteMeetingType({ path: { id } });
    if (result.error) {
      setFormError('Не удалось удалить тип встречи.');
      return;
    }
    if (editingId === id) resetForm();
    await loadMeetingTypes();
  };

  return (
    <div className="admin-page">
      <span className="guest-badge">
        <span className="guest-name">{guest.name || 'Гость'}</span>
      </span>
      <h1 className="page-heading">Админка</h1>
      <p className="page-hint">
        Владелец календаря управляет типами встреч и просматривает записи.
      </p>

      <section className="admin-section">
        <h2>Типы встреч</h2>

        <form className="admin-form" onSubmit={submit}>
          <label>
            Название
            <input
              value={form.name}
              onChange={(event) => setForm({ ...form, name: event.target.value })}
              required
            />
          </label>
          <label>
            Описание
            <input
              value={form.description}
              onChange={(event) => setForm({ ...form, description: event.target.value })}
              required
            />
          </label>
          <label>
            Длительность, мин
            <input
              type="number"
              min={5}
              max={240}
              value={form.durationMinutes}
              onChange={(event) => setForm({ ...form, durationMinutes: event.target.value })}
              required
            />
          </label>
          <div className="admin-form-actions">
            <button type="submit" className="button button-primary" disabled={saving}>
              {saving ? 'Сохраняем…' : editingId ? 'Сохранить тип' : 'Добавить тип'}
            </button>
            {editingId && (
              <button type="button" className="button" onClick={resetForm}>
                Отмена
              </button>
            )}
          </div>
          {formError && <p className="hint-error">{formError}</p>}
        </form>

        <ul className="meeting-types admin-types">
          {meetingTypes.map((t) => (
            <li key={t.id} className="admin-type-row">
              <span style={{ fontWeight: 600 }}>{t.name}</span>
              <span style={{ marginLeft: 12, color: '#5c657a' }}>— {t.description}</span>
              <span style={{ marginLeft: 24, color: '#3b5bdb' }}>({t.durationMinutes} мин)</span>
              <span className="admin-type-actions">
                <button type="button" className="button" onClick={() => startEdit(t)}>
                  Редактировать
                </button>
                <button type="button" className="button" onClick={() => void remove(t.id)}>
                  Удалить
                </button>
              </span>
            </li>
          ))}
        </ul>
        {meetingTypes.length === 0 && (
          <p className="empty-admin">Типы встреч не найдены. Добавьте их в админке.</p>
        )}
      </section>

      <section className="admin-section" style={{ marginTop: '32px' }}>
        <h2>Предстоящие записи</h2>
        {bookings.length === 0 ? (
          <p className="empty-admin">Записей пока нет.</p>
        ) : (
          <table className="admin-table">
            <thead>
              <tr>
                <th>Дата</th>
                <th>Время</th>
                <th>Гость</th>
                <th>Тип встречи</th>
              </tr>
            </thead>
            <tbody>
              {bookings.map((b) => (
                <tr key={b.id}>
                  <td>{b.date}</td>
                  <td>
                    {b.startTime.slice(0, 5)} – {b.endTime.slice(0, 5)}
                  </td>
                  <td>
                    <span>{b.guestName}</span>
                    <span>{b.guestAvatar}</span>
                  </td>
                  <td>{b.meetingTypeName}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
