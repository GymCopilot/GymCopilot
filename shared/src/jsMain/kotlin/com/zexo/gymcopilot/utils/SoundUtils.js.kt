package com.zexo.gymcopilot.utils

actual fun playTickSound() {
    try {
        js("""
            try {
                var ctx = window.audioCtx || (window.audioCtx = new (window.AudioContext || window.webkitAudioContext)());
                if (ctx.state === 'suspended') { ctx.resume(); }
                var osc = ctx.createOscillator();
                var gain = ctx.createGain();
                osc.type = 'sine';
                osc.frequency.setValueAtTime(800, ctx.currentTime);
                gain.gain.setValueAtTime(0.05, ctx.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.02);
                osc.connect(gain);
                gain.connect(ctx.destination);
                osc.start();
                osc.stop(ctx.currentTime + 0.02);
            } catch(e) {}
        """)
    } catch (e: Exception) {}
}
