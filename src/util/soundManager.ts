class SoundManager {
  private isEnabled = true;
  private audioCtx: AudioContext | null = null;
  private popBuffer: AudioBuffer | null = null;
  private chimeBuffer: AudioBuffer | null = null;

  constructor() {
    // Lazily initialized on first user interaction to comply with Web Audio autoplay policies
  }

  public setSoundEnabled(enabled: boolean) {
    this.isEnabled = enabled;
  }

  public getSoundEnabled(): boolean {
    return this.isEnabled;
  }

  private getAudioContext(): AudioContext | null {
    if (typeof window === 'undefined') return null;
    try {
      const AudioContextClass =
        window.AudioContext ||
        (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (!this.audioCtx && AudioContextClass) {
        this.audioCtx = new AudioContextClass();
      }
      if (this.audioCtx && this.audioCtx.state === 'suspended') {
        this.audioCtx.resume();
      }
      return this.audioCtx;
    } catch {
      return null;
    }
  }

  /**
   * Pre-renders crisp high-pitch 15ms pop (2500Hz) with instant exponential decay
   * Strictly NO low-frequency metallic / reverberating "ကွမ်" tone.
   */
  private getOrCreatePopBuffer(ctx: AudioContext): AudioBuffer {
    if (this.popBuffer) return this.popBuffer;
    const sampleRate = ctx.sampleRate;
    const durationMs = 15;
    const numSamples = Math.floor((durationMs * sampleRate) / 1000);
    const buffer = ctx.createBuffer(1, numSamples, sampleRate);
    const data = buffer.getChannelData(0);

    for (let i = 0; i < numSamples; i++) {
      const progress = i / numSamples;
      const env = Math.exp(-12.0 * progress);
      const s = Math.sin(2.0 * Math.PI * 2500.0 * (i / sampleRate)) * env * 0.3;
      data[i] = s;
    }

    this.popBuffer = buffer;
    return buffer;
  }

  /**
   * Dual Chime: 150ms split chime (1046.5Hz C6 -> 1318.5Hz E6)
   * "တီး-လင်" ဘောင်ချာအောင်မြင်သံ
   */
  private getOrCreateChimeBuffer(ctx: AudioContext): AudioBuffer {
    if (this.chimeBuffer) return this.chimeBuffer;
    const sampleRate = ctx.sampleRate;
    const totalMs = 150;
    const numSamples = Math.floor((totalMs * sampleRate) / 1000);
    const split = Math.floor(0.05 * sampleRate);
    const buffer = ctx.createBuffer(1, numSamples, sampleRate);
    const data = buffer.getChannelData(0);

    for (let i = 0; i < numSamples; i++) {
      const t = i / sampleRate;
      let sample = 0;
      if (i < split) {
        const env = Math.exp(-6.0 * (i / split));
        sample = Math.sin(2.0 * Math.PI * 1046.5 * t) * env * 0.35;
      } else {
        const env = Math.exp(-7.0 * ((i - split) / (numSamples - split)));
        sample = Math.sin(2.0 * Math.PI * 1318.5 * t) * env * 0.4;
      }
      data[i] = sample;
    }

    this.chimeBuffer = buffer;
    return buffer;
  }

  /**
   * Mobile UI Tap ("တစ်" အသံတိုလေး - Strictly NO "ကွမ်" sound)
   */
  public playTouchClick() {
    if (!this.isEnabled) return;
    try {
      const ctx = this.getAudioContext();
      if (!ctx) return;
      const buffer = this.getOrCreatePopBuffer(ctx);
      const source = ctx.createBufferSource();
      source.buffer = buffer;
      source.connect(ctx.destination);
      source.start();
    } catch {
      // Ignore
    }
  }

  /**
   * Voucher Saved Chime ("တီး-လင်" ဘောင်ချာအောင်မြင်သံ)
   */
  public playComplete() {
    if (!this.isEnabled) return;
    try {
      const ctx = this.getAudioContext();
      if (!ctx) return;
      const buffer = this.getOrCreateChimeBuffer(ctx);
      const source = ctx.createBufferSource();
      source.buffer = buffer;
      source.connect(ctx.destination);
      source.start();

      if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
        navigator.vibrate(25);
      }
    } catch {
      // Ignore
    }
  }

  // Aliases ensuring every click route uses crisp pop
  public playClick() {
    this.playTouchClick();
  }

  public playTap() {
    this.playTouchClick();
  }

  public playEdit() {
    this.playTouchClick();
  }

  public playDelete() {
    this.playTouchClick();
  }

  public playSave() {
    this.playTouchClick();
  }

  public playChime() {
    this.playComplete();
  }
}

export const soundManager = new SoundManager();
export const AppSoundManager2D = soundManager;
