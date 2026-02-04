<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  fetchAvailability,
  updateAvailability,
} from '../../services/teacherAvailabilityService';
import type {
  AvailabilityResponse,
  AvailabilitySlotRequest,
  AvailabilityStatus,
  FranjaHorarioDto,
} from '../../types/availability';

const dayOptions = [
  { label: 'Sáb', value: 6 },
  { label: 'Dom', value: 7 },
  { label: 'Lun', value: 1 },
  { label: 'Mar', value: 2 },
  { label: 'Mié', value: 3 },
  { label: 'Jue', value: 4 },
  { label: 'Vie', value: 5 },
];

const quickDays = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];
const blockOptions = ['Seleccionar bloque', 'Bloque Mañana', 'Bloque Tarde'];

const selectedDay = ref('Lun');
const selectedBlock = ref(blockOptions[0]);
const feedback = ref('');
const feedbackType = ref<'info' | 'success'>('info');
const isSaving = ref(false);
const lastSavedAt = ref<string | null>(null);
const periodoId = ref<number | null>(null);
const franjas = ref<FranjaHorarioDto[]>([]);
const slotStatus = ref<Record<string, AvailabilityStatus>>({});

const buildKey = (diaSemana: number, franjaId: number) => `${diaSemana}-${franjaId}`;

const days = computed(() => dayOptions.map((day) => day.label));

const getDayValue = (label: string) => dayOptions.find((day) => day.label === label)?.value ?? 0;

const getSlotStatus = (dayLabel: string, franjaId: number) => {
  const diaSemana = getDayValue(dayLabel);
  return slotStatus.value[buildKey(diaSemana, franjaId)] ?? 'NO_DISPONIBLE';
};

const getStatusClass = (dayLabel: string, franjaId: number) => {
  return getSlotStatus(dayLabel, franjaId).toLowerCase();
};

const toggleSlot = (dayLabel: string, franjaId: number) => {
  const diaSemana = getDayValue(dayLabel);
  const key = buildKey(diaSemana, franjaId);
  const current = slotStatus.value[key] ?? 'NO_DISPONIBLE';
  if (current === 'SESION') return;
  slotStatus.value[key] = current === 'DISPONIBLE' ? 'NO_DISPONIBLE' : 'DISPONIBLE';
};

const clearAll = () => {
  const updated: Record<string, AvailabilityStatus> = { ...slotStatus.value };
  Object.keys(updated).forEach((key) => {
    if (updated[key] !== 'SESION') {
      updated[key] = 'NO_DISPONIBLE';
    }
  });
  slotStatus.value = updated;
  feedback.value = 'Disponibilidad restablecida.';
  feedbackType.value = 'info';
};

const buildPayload = (): AvailabilitySlotRequest[] => {
  const payload: AvailabilitySlotRequest[] = [];
  dayOptions.forEach((day) => {
    franjas.value.forEach((franja) => {
      payload.push({
        diaSemana: day.value,
        franjaId: franja.franjaId,
        status: slotStatus.value[buildKey(day.value, franja.franjaId)] ?? 'NO_DISPONIBLE',
      });
    });
  });
  return payload;
};

const saveChanges = async () => {
  if (isSaving.value) return;
  feedback.value = '';
  feedbackType.value = 'info';
  isSaving.value = true;
  try {
    const response = await updateAvailability({
      periodoId: periodoId.value,
      slots: buildPayload(),
    });
    feedback.value = response.message ?? 'Disponibilidad actualizada.';
    feedbackType.value = 'success';
    lastSavedAt.value = new Date().toLocaleString();
    await loadAvailability();
  } catch (error) {
    feedback.value = 'No se pudo guardar la disponibilidad.';
    feedbackType.value = 'info';
  } finally {
    isSaving.value = false;
  }
};

const selectDay = (day: string) => {
  selectedDay.value = day;
  const diaSemana = getDayValue(day);
  const updated: Record<string, AvailabilityStatus> = { ...slotStatus.value };
  franjas.value.forEach((franja) => {
    const key = buildKey(diaSemana, franja.franjaId);
    if (updated[key] !== 'SESION') {
      updated[key] = 'DISPONIBLE';
    }
  });
  slotStatus.value = updated;
};

const selectBlock = (event: Event) => {
  const target = event.target as HTMLSelectElement;
  selectedBlock.value = target.value;
};

const getDaySlots = (dayLabel: string) => {
  const diaSemana = getDayValue(dayLabel);
  return franjas.value.filter((franja) => {
    const status = slotStatus.value[buildKey(diaSemana, franja.franjaId)] ?? 'NO_DISPONIBLE';
    return status === 'DISPONIBLE';
  }).length;
};

const stats = computed(() => {
  let disponibles = 0;
  let programadas = 0;
  const distribucion = dayOptions.map((day) => {
    let hours = 0;
    franjas.value.forEach((franja) => {
      const status = slotStatus.value[buildKey(day.value, franja.franjaId)] ?? 'NO_DISPONIBLE';
      if (status === 'DISPONIBLE') {
        disponibles += 1;
        hours += 1;
      }
      if (status === 'SESION') {
        programadas += 1;
      }
    });
    return { day: day.label, hours };
  });

  const diasActivos = distribucion.filter((item) => item.hours > 0).length;
  return {
    disponibles,
    programadas,
    horasSemanales: disponibles,
    diasActivos,
    distribucion,
  };
});

const loadAvailability = async () => {
  try {
    const data: AvailabilityResponse = await fetchAvailability();
    periodoId.value = data.periodoId ?? null;
    franjas.value = [...data.franjas].sort((a, b) => a.horaInicio.localeCompare(b.horaInicio));
    const updated: Record<string, AvailabilityStatus> = {};
    data.slots.forEach((slot) => {
      updated[buildKey(slot.diaSemana, slot.franjaId)] = slot.status;
    });
    slotStatus.value = updated;
  } catch (error) {
    feedback.value = 'No se pudo cargar la disponibilidad.';
    feedbackType.value = 'info';
  }
};

onMounted(() => {
  loadAvailability();
});
</script>

<template>
  <section class="page-header">
    <div>
      <h2><span class="header-icon">🕘</span> Gestión de Disponibilidad</h2>
      <p>Define tus horarios disponibles para sesiones de refuerzo académico.</p>
    </div>
    <div class="header-actions">
      <button class="btn btn-outline-secondary btn-sm" type="button" @click="clearAll" :disabled="isSaving">
        Limpiar todo
      </button>
      <button class="btn btn-success btn-sm" type="button" @click="saveChanges" :disabled="isSaving">
        {{ isSaving ? 'Guardando...' : 'Guardar cambios' }}
      </button>
    </div>
  </section>

  <p class="save-status" v-if="lastSavedAt">Último guardado: {{ lastSavedAt }}</p>

  <section class="info-card">
    <div class="info-icon">ℹ️</div>
    <div>
      <strong>Instrucciones:</strong> Haz clic en las celdas para marcar o desmarcar tu disponibilidad.
      Los horarios marcados en verde estarán disponibles para que los estudiantes soliciten sesiones de refuerzo.
      <div class="info-note">
        Los horarios ya reservados para sesiones aparecen en color morado y no pueden modificarse.
      </div>
    </div>
  </section>

  <section class="legend-card">
    <span class="legend-item"><span class="legend-chip available"></span> Disponible</span>
    <span class="legend-item"><span class="legend-chip busy"></span> No disponible</span>
    <span class="legend-item"><span class="legend-chip session"></span> Sesión programada</span>
  </section>

  <section class="controls-card">
    <div class="control-group">
      <span class="control-label">Selección rápida por día:</span>
      <div class="day-buttons">
        <button
          v-for="day in quickDays"
          :key="day"
          class="day-button"
          type="button"
          :class="{ active: selectedDay === day }"
          @click="selectDay(day)"
        >
          {{ day }}
        </button>
      </div>
    </div>
    <div class="control-group">
      <span class="control-label">Selección por bloque horario:</span>
      <select class="block-select" :value="selectedBlock" @change="selectBlock">
        <option v-for="option in blockOptions" :key="option" :value="option">{{ option }}</option>
      </select>
    </div>
  </section>

  <section class="availability-card">
    <div class="availability-grid">
      <div class="grid-header">
        <div class="time-cell"></div>
        <div v-for="day in days" :key="day" class="day-cell">
          <span class="day-name">{{ day }}</span>
          <small>{{ getDaySlots(day) }} slots</small>
        </div>
      </div>
      <div v-for="franja in franjas" :key="franja.franjaId" class="grid-row">
        <div class="time-cell">
          <strong>{{ franja.horaInicio }}</strong>
          <small>{{ franja.horaFin }}</small>
        </div>
        <button
          v-for="day in days"
          :key="`${day}-${franja.franjaId}`"
          class="slot"
          type="button"
          :class="getStatusClass(day, franja.franjaId)"
          @click="toggleSlot(day, franja.franjaId)"
        >
          <span v-if="getSlotStatus(day, franja.franjaId) === 'DISPONIBLE'" class="slot-icon">✓</span>
          <span v-if="getSlotStatus(day, franja.franjaId) === 'SESION'" class="slot-icon">✕</span>
        </button>
      </div>
    </div>
  </section>

  <div v-if="feedback" class="toast" :class="{ 'toast-success': feedbackType === 'success' }">
    {{ feedback }}
  </div>

  <section class="summary-card">
    <div class="summary-header">
      <span class="summary-icon">📋</span>
      <h4>Resumen de Disponibilidad</h4>
    </div>
    <div class="summary-metrics">
      <div class="summary-item">
        <span class="summary-value">{{ stats.disponibles }}</span>
        <span class="summary-label">Slots disponibles</span>
      </div>
      <div class="summary-item">
        <span class="summary-value">{{ stats.programadas }}</span>
        <span class="summary-label">Sesiones programadas</span>
      </div>
      <div class="summary-item">
        <span class="summary-value">{{ stats.horasSemanales }}</span>
        <span class="summary-label">Horas semanales</span>
      </div>
      <div class="summary-item">
        <span class="summary-value">{{ stats.diasActivos }}</span>
        <span class="summary-label">Días con disponibilidad</span>
      </div>
    </div>
    <div class="distribution">
      <h5>Distribución semanal</h5>
      <div v-for="item in stats.distribucion" :key="item.day" class="bar">
        <span>{{ item.day }}</span>
        <div class="bar-track">
          <div class="bar-fill" :style="{ width: `${item.hours * 14}%` }"></div>
        </div>
        <strong>{{ item.hours }}</strong>
      </div>
    </div>
  </section>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0 0 4px;
  font-weight: 600;
  font-size: 22px;
  color: #1f2b24;
}

.page-header p {
  margin: 0;
  color: #607169;
  font-size: 13px;
}

.header-icon {
  color: #1d4ed8;
  margin-right: 6px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.save-status {
  margin: 0 0 12px;
  color: #4b5563;
  font-size: 12px;
  text-align: right;
}

.info-card {
  background: #eef0fb;
  border: 1px solid #d9def1;
  border-left: 4px solid #3b5bd6;
  border-radius: 12px;
  padding: 14px;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 10px;
  font-size: 13px;
  color: #39445f;
  margin-bottom: 12px;
}

.info-note {
  margin-top: 6px;
  font-size: 12px;
  color: #5a6585;
}

.info-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: #dfe6ff;
  display: grid;
  place-items: center;
  font-size: 16px;
}

.legend-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e3ebe6;
  padding: 10px 14px;
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #54645c;
  margin-bottom: 12px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.legend-chip {
  width: 14px;
  height: 14px;
  border-radius: 4px;
}

.legend-chip.available {
  background: #4caf50;
}

.legend-chip.busy {
  background: #e5e7eb;
}

.legend-chip.session {
  background: #8b2bbf;
}

.controls-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e3ebe6;
  padding: 12px 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 14px;
  align-items: center;
}

.control-group {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #5c6b64;
}

.control-label {
  font-weight: 600;
}

.day-buttons {
  display: flex;
  gap: 6px;
}

.day-button {
  border: 1px solid #dbe4dd;
  background: #ffffff;
  border-radius: 8px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 600;
  color: #334039;
}

.day-button.active {
  background: #1f3b91;
  color: #fff;
  border-color: #1f3b91;
}

.block-select {
  border-radius: 8px;
  padding: 6px 10px;
  border: 1px solid #dbe4dd;
  font-size: 12px;
  color: #334039;
  background: #fff;
}

.availability-card {
  background: #ffffff;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid #e3ebe6;
  box-shadow: 0 6px 12px rgba(15, 23, 42, 0.05);
  margin-bottom: 16px;
}

.availability-grid {
  display: grid;
  gap: 8px;
}

.grid-header,
.grid-row {
  display: grid;
  grid-template-columns: 80px 70px repeat(5, 1fr);
  gap: 8px;
}

.time-cell {
  font-size: 11px;
  color: #5c6b64;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
}

.day-cell {
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: #1f2e84;
  border-radius: 8px;
  padding: 6px 4px;
  display: grid;
  gap: 2px;
}

.day-cell small {
  font-weight: 500;
  font-size: 10px;
  opacity: 0.8;
}

.slot {
  height: 32px;
  border-radius: 8px;
  border: 1px solid #eef1f4;
  background: #f7f7f8;
  display: grid;
  place-items: center;
  font-size: 12px;
  color: #fff;
}

.slot.disponible {
  background: #4caf50;
  border-color: #4caf50;
}

.slot.no_disponible {
  background: #f7f7f8;
  border-color: #eef1f4;
  color: transparent;
}

.slot.sesion {
  background: #8b2bbf;
  border-color: #8b2bbf;
  cursor: not-allowed;
}

.slot-icon {
  font-size: 12px;
}

.toast {
  position: fixed;
  right: 24px;
  bottom: 24px;
  background: #1f2937;
  color: #fff;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 12px;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.2);
  z-index: 50;
}

.toast-success {
  background: #1f7a46;
}

.summary-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e3ebe6;
  padding: 16px;
  box-shadow: 0 6px 12px rgba(15, 23, 42, 0.05);
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.summary-header h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.summary-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-item {
  display: grid;
  gap: 4px;
  font-size: 12px;
  color: #5c6b64;
}

.summary-value {
  font-size: 18px;
  font-weight: 700;
  color: #1f2b24;
}

.distribution h5 {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 600;
}

.bar {
  display: grid;
  grid-template-columns: 40px 1fr 20px;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: #5c6b64;
  margin-bottom: 6px;
}

.bar-track {
  height: 8px;
  background: #f0f2f5;
  border-radius: 999px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: #4c63d2;
  border-radius: 999px;
}

@media (max-width: 900px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .grid-header,
  .grid-row {
    grid-template-columns: 80px repeat(6, minmax(60px, 1fr));
  }
}
</style>
