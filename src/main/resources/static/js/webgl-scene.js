/**
 * CloudNest Enterprise — WebGL Scene Engine
 * ==========================================
 * Centralized Three.js r167 scene factories for all pages.
 *
 * Scenes:
 *  A. HeroAuthScene         — Login/Register galaxy spiral with bloom
 *  B. NodeTopologyScene     — Nodes page PBR topology with post-processing
 *  C. DashboardHeroBanner   — Dashboard animated torus-knot banner
 *  D. GlobalParticleBackground — Lightweight ambient particle field
 *
 * Each factory returns a { destroy() } handle for cleanup.
 *
 * Usage:
 *   const scene = CloudNestWebGL.HeroAuthScene('canvasId', { theme: 'blue' });
 *   // later:
 *   scene.destroy();
 */

const CloudNestWebGL = (() => {
    'use strict';

    // ── Guards ────────────────────────────────────────────────────────────────
    if (typeof THREE === 'undefined') {
        console.warn('[CloudNestWebGL] Three.js not loaded. Scenes disabled.');
        return {
            HeroAuthScene: () => ({ destroy: () => {} }),
            NodeTopologyScene: () => ({ destroy: () => {} }),
            DashboardHeroBanner: () => ({ destroy: () => {} }),
            GlobalParticleBackground: () => ({ destroy: () => {} }),
        };
    }

    // ── Performance tier detection ─────────────────────────────────────────
    const IS_LOW_END = navigator.hardwareConcurrency !== undefined && navigator.hardwareConcurrency < 4;
    const IS_MOBILE  = /Android|iPhone|iPad/i.test(navigator.userAgent);
    const BLOOM_ENABLED = !IS_LOW_END && !IS_MOBILE;

    // ── Shared utility ────────────────────────────────────────────────────────
    function lerp(a, b, t) { return a + (b - a) * t; }

    function setupRenderer(canvas, clearColor = 0x080E1C, alpha = false) {
        const renderer = new THREE.WebGLRenderer({ canvas, antialias: !IS_MOBILE, alpha });
        renderer.setPixelRatio(Math.min(window.devicePixelRatio, IS_MOBILE ? 1.5 : 2));
        renderer.setClearColor(clearColor, alpha ? 0 : 1);
        renderer.toneMapping = THREE.ACESFilmicToneMapping;
        renderer.toneMappingExposure = 1.2;
        return renderer;
    }

    function makeResizer(renderer, camera, canvas) {
        function resize() {
            const w = canvas.clientWidth || canvas.offsetWidth;
            const h = canvas.clientHeight || canvas.offsetHeight;
            if (w === 0 || h === 0) return;
            renderer.setSize(w, h, false);
            camera.aspect = w / h;
            camera.updateProjectionMatrix();
        }
        resize();
        const ro = new ResizeObserver(resize);
        ro.observe(canvas);
        return () => ro.disconnect();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // A. HERO AUTH SCENE — Galaxy Spiral (Login / Register)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    function HeroAuthScene(canvasId, options = {}) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return { destroy: () => {} };

        const theme = options.theme || 'blue'; // 'blue' | 'purple'
        const CORE_COLOR   = theme === 'purple' ? 0x8B5CF6 : 0x3B82F6;
        const OUTER_COLOR  = theme === 'purple' ? 0x3B82F6 : 0x8B5CF6;
        const ACCENT_COLOR = theme === 'purple' ? 0xEC4899 : 0x22D3EE;

        const renderer = setupRenderer(canvas, 0x080E1C);
        const scene    = new THREE.Scene();
        scene.fog      = new THREE.FogExp2(0x080E1C, 0.018);

        const camera = new THREE.PerspectiveCamera(65, 1, 0.1, 120);
        camera.position.set(0, 2, 10);

        const stopResize = makeResizer(renderer, camera, canvas);

        // ── Lights ────────────────────────────────────────────────────────
        const ambient = new THREE.AmbientLight(CORE_COLOR, 0.25);
        scene.add(ambient);

        const pLight1 = new THREE.PointLight(CORE_COLOR, 4, 25);
        pLight1.position.set(4, 4, 4);
        scene.add(pLight1);

        const pLight2 = new THREE.PointLight(OUTER_COLOR, 3, 20);
        pLight2.position.set(-5, -3, 3);
        scene.add(pLight2);

        const pLight3 = new THREE.PointLight(ACCENT_COLOR, 2, 15);
        pLight3.position.set(0, 6, -4);
        scene.add(pLight3);

        // ── Galaxy Spiral (5,000 particles) ──────────────────────────────
        const PARTICLE_COUNT = IS_MOBILE ? 2000 : 5000;
        const positions  = new Float32Array(PARTICLE_COUNT * 3);
        const colors     = new Float32Array(PARTICLE_COUNT * 3);
        const sizes      = new Float32Array(PARTICLE_COUNT);

        const colorCore  = new THREE.Color(CORE_COLOR);
        const colorOuter = new THREE.Color(OUTER_COLOR);
        const colorAccent = new THREE.Color(ACCENT_COLOR);

        for (let i = 0; i < PARTICLE_COUNT; i++) {
            // Logarithmic spiral distribution
            const arm      = i % 3; // 3 spiral arms
            const t        = (i / PARTICLE_COUNT);
            const r        = 0.3 + t * 5.5;
            const angle    = t * Math.PI * 10 + (arm * (Math.PI * 2 / 3));
            const scatter  = (Math.random() - 0.5) * r * 0.45;

            positions[i * 3]     = Math.cos(angle) * r + scatter;
            positions[i * 3 + 1] = (Math.random() - 0.5) * r * 0.3;
            positions[i * 3 + 2] = Math.sin(angle) * r + scatter;

            // Color gradient: core → outer → accent at tips
            let mixColor;
            if (t < 0.4)      mixColor = colorCore.clone().lerp(colorOuter, t / 0.4);
            else if (t < 0.75) mixColor = colorOuter.clone().lerp(colorAccent, (t - 0.4) / 0.35);
            else               mixColor = colorAccent.clone().lerp(new THREE.Color(0xffffff), (t - 0.75) / 0.25);

            colors[i * 3]     = mixColor.r;
            colors[i * 3 + 1] = mixColor.g;
            colors[i * 3 + 2] = mixColor.b;

            sizes[i] = 0.012 + Math.random() * 0.025 * (1 - t * 0.5);
        }

        const galaxyGeo = new THREE.BufferGeometry();
        galaxyGeo.setAttribute('position', new THREE.BufferAttribute(positions, 3));
        galaxyGeo.setAttribute('color', new THREE.BufferAttribute(colors, 3));
        galaxyGeo.setAttribute('size', new THREE.BufferAttribute(sizes, 1));

        const galaxyMat = new THREE.PointsMaterial({
            vertexColors: true,
            size: 0.04,
            sizeAttenuation: true,
            transparent: true,
            opacity: 0.88,
            depthWrite: false,
            blending: THREE.AdditiveBlending,
        });

        const galaxy = new THREE.Points(galaxyGeo, galaxyMat);
        scene.add(galaxy);

        // ── Three Orbiting Infrastructure Nodes ──────────────────────────
        const NODE_DEFS = [
            { pos: [-2.2, 0.5, 0],  color: CORE_COLOR,   label: 'ALPHA' },
            { pos: [ 2.2, 0.5, 0],  color: OUTER_COLOR,  label: 'BETA'  },
            { pos: [ 0,  -1.8, 0.5], color: ACCENT_COLOR, label: 'GAMMA' },
        ];

        const nodeGeo   = new THREE.IcosahedronGeometry(0.22, 2);
        const nodeMeshes = [];

        NODE_DEFS.forEach(nd => {
            const mat = new THREE.MeshStandardMaterial({
                color: nd.color,
                emissive: nd.color,
                emissiveIntensity: 0.9,
                metalness: 0.7,
                roughness: 0.2,
                transparent: true,
                opacity: 0.95,
            });
            const mesh = new THREE.Mesh(nodeGeo, mat);
            mesh.position.set(...nd.pos);
            mesh.userData.basePos = [...nd.pos];
            mesh.userData.color   = nd.color;
            scene.add(mesh);
            nodeMeshes.push(mesh);

            // Glow ring
            const ringGeo = new THREE.RingGeometry(0.28, 0.33, 48);
            const ringMat = new THREE.MeshBasicMaterial({
                color: nd.color,
                transparent: true,
                opacity: 0.35,
                side: THREE.DoubleSide,
                depthWrite: false,
                blending: THREE.AdditiveBlending,
            });
            const ring = new THREE.Mesh(ringGeo, ringMat);
            ring.rotation.x = -Math.PI / 2;
            ring.position.set(...nd.pos);
            scene.add(ring);
            mesh.userData.ring = ring;
        });

        // ── Edge Lines Between Nodes ──────────────────────────────────────
        [[0,1],[1,2],[0,2]].forEach(([a, b]) => {
            const pts = [
                new THREE.Vector3(...NODE_DEFS[a].pos),
                new THREE.Vector3(...NODE_DEFS[b].pos),
            ];
            const geo = new THREE.BufferGeometry().setFromPoints(pts);
            const mat = new THREE.LineBasicMaterial({
                color: CORE_COLOR,
                transparent: true,
                opacity: 0.18,
                depthWrite: false,
                blending: THREE.AdditiveBlending,
            });
            scene.add(new THREE.Line(geo, mat));
        });

        // ── Data Stream Lines (shooting from center outward) ──────────────
        const STREAM_COUNT = IS_MOBILE ? 8 : 18;
        const streams = [];
        for (let i = 0; i < STREAM_COUNT; i++) {
            const angle  = (i / STREAM_COUNT) * Math.PI * 2;
            const length = 1.5 + Math.random() * 3;
            const pts    = [
                new THREE.Vector3(0, 0, 0),
                new THREE.Vector3(Math.cos(angle) * length, (Math.random()-0.5)*0.6, Math.sin(angle) * length),
            ];
            const geo = new THREE.BufferGeometry().setFromPoints(pts);
            const mat = new THREE.LineBasicMaterial({
                color: i % 2 === 0 ? CORE_COLOR : OUTER_COLOR,
                transparent: true,
                opacity: 0,
                depthWrite: false,
                blending: THREE.AdditiveBlending,
            });
            const line = new THREE.Line(geo, mat);
            line.userData.delay  = Math.random() * Math.PI * 2;
            line.userData.speed  = 0.8 + Math.random() * 1.2;
            scene.add(line);
            streams.push(line);
        }

        // ── Mouse Parallax ────────────────────────────────────────────────
        let mx = 0, my = 0, tx = 0, ty = 0;
        const onMouseMove = e => {
            tx = (e.clientX / window.innerWidth  - 0.5) * 2;
            ty = (e.clientY / window.innerHeight - 0.5) * 2;
        };
        document.addEventListener('mousemove', onMouseMove);

        // ── Animation Loop ────────────────────────────────────────────────
        const clock = new THREE.Clock();
        let raf;

        function animate() {
            raf = requestAnimationFrame(animate);
            if (document.hidden) return;

            const t = clock.getElapsedTime();

            // Smooth mouse lerp
            mx = lerp(mx, tx, 0.04);
            my = lerp(my, ty, 0.04);

            // Galaxy slow rotation
            galaxy.rotation.y = t * 0.025 + mx * 0.06;
            galaxy.rotation.x = my * 0.03;

            // Pulse nodes + rotate rings
            nodeMeshes.forEach((mesh, i) => {
                const pulse = 1 + Math.sin(t * 1.8 + i * 1.4) * 0.08;
                mesh.scale.setScalar(pulse);
                mesh.material.emissiveIntensity = 0.7 + Math.sin(t * 2 + i) * 0.3;
                if (mesh.userData.ring) {
                    mesh.userData.ring.rotation.z = t * 0.4 + i;
                    mesh.userData.ring.material.opacity = 0.2 + Math.sin(t * 1.5 + i) * 0.15;
                }
            });

            // Data streams flash in/out
            streams.forEach(s => {
                const v = Math.sin(t * s.userData.speed + s.userData.delay);
                s.material.opacity = Math.max(0, v) * 0.6;
            });

            // Camera subtle drift
            camera.position.x = lerp(camera.position.x, mx * 0.8, 0.02);
            camera.position.y = lerp(camera.position.y, 2 - my * 0.4, 0.02);

            renderer.render(scene, camera);
        }

        animate();

        // Pause/resume on tab visibility
        const onVisibility = () => {
            if (document.hidden) { cancelAnimationFrame(raf); }
            else { animate(); }
        };
        document.addEventListener('visibilitychange', onVisibility);

        // WebGL context loss
        canvas.addEventListener('webglcontextlost', e => { e.preventDefault(); cancelAnimationFrame(raf); });
        canvas.addEventListener('webglcontextrestored', () => { animate(); });

        return {
            destroy() {
                cancelAnimationFrame(raf);
                stopResize();
                document.removeEventListener('mousemove', onMouseMove);
                document.removeEventListener('visibilitychange', onVisibility);
                renderer.dispose();
            }
        };
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // B. NODE TOPOLOGY SCENE — PBR Materials + Data Packets (Nodes Page)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    function NodeTopologyScene(canvasId, options = {}) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return { destroy: () => {} };

        const renderer = setupRenderer(canvas, 0x0B1120);
        const scene    = new THREE.Scene();
        scene.fog      = new THREE.FogExp2(0x0B1120, 0.022);

        const camera = new THREE.PerspectiveCamera(55, 1, 0.1, 100);
        camera.position.set(0, 1.5, 8);

        const stopResize = makeResizer(renderer, camera, canvas);

        // ── Lights ────────────────────────────────────────────────────────
        scene.add(new THREE.AmbientLight(0x3B82F6, 0.3));

        const lights = [
            { color: 0x3B82F6, pos: [0, 5, 5],  intensity: 4 },
            { color: 0x8B5CF6, pos: [-5,-3, 2],  intensity: 3 },
            { color: 0x22D3EE, pos: [5, 2, -3],  intensity: 2 },
        ];
        lights.forEach(l => {
            const pl = new THREE.PointLight(l.color, l.intensity, 22);
            pl.position.set(...l.pos);
            scene.add(pl);
        });

        // ── Node Definitions ──────────────────────────────────────────────
        const NODE_DATA = [
            { name: 'Alpha', pos: [-2.6, 1.2, 0],   color: 0x3B82F6, emissive: 0x1d4ed8, label: 'labelAlpha' },
            { name: 'Beta',  pos: [ 2.6, 1.2, 0],   color: 0x8B5CF6, emissive: 0x5b21b6, label: 'labelBeta'  },
            { name: 'Gamma', pos: [ 0,  -2, 0.6],   color: 0x22C55E, emissive: 0x166534, label: 'labelGamma' },
        ];

        // ── Node Meshes (PBR MeshStandardMaterial) ────────────────────────
        const nodeGeo    = new THREE.IcosahedronGeometry(0.48, 3);
        const nodeMeshes = [];
        const nodeLabels = NODE_DATA.map(nd => document.getElementById(nd.label));

        NODE_DATA.forEach((nd, i) => {
            const mat = new THREE.MeshStandardMaterial({
                color: nd.color,
                emissive: nd.emissive,
                emissiveIntensity: 0.8,
                metalness: 0.6,
                roughness: 0.25,
                transparent: true,
                opacity: 0.96,
            });

            const mesh = new THREE.Mesh(nodeGeo, mat);
            mesh.position.set(...nd.pos);
            mesh.userData = { index: i, name: nd.name, baseScale: 1 };
            scene.add(mesh);
            nodeMeshes.push(mesh);

            // Outer wireframe shell for depth
            const wireMat = new THREE.MeshBasicMaterial({
                color: nd.color,
                wireframe: true,
                transparent: true,
                opacity: 0.08,
            });
            const wire = new THREE.Mesh(new THREE.IcosahedronGeometry(0.52, 2), wireMat);
            wire.position.copy(mesh.position);
            scene.add(wire);
            mesh.userData.wire = wire;

            // Glow rings (3 concentric rings per node)
            [0.62, 0.72, 0.85].forEach((r, ri) => {
                const rGeo = new THREE.RingGeometry(r, r + 0.025, 64);
                const rMat = new THREE.MeshBasicMaterial({
                    color: nd.color,
                    transparent: true,
                    opacity: 0.18 - ri * 0.05,
                    side: THREE.DoubleSide,
                    depthWrite: false,
                    blending: THREE.AdditiveBlending,
                });
                const ring = new THREE.Mesh(rGeo, rMat);
                ring.rotation.x = -Math.PI / 2;
                ring.position.copy(mesh.position);
                scene.add(ring);
                if (!mesh.userData.rings) mesh.userData.rings = [];
                mesh.userData.rings.push(ring);
            });

            if (nodeLabels[i]) nodeLabels[i].style.display = 'block';
        });

        // ── Edge Lines (Dashed Look via Dash material) ─────────────────────
        const EDGE_PAIRS = [[0,1],[1,2],[0,2]];
        const edgeMeshes = [];

        EDGE_PAIRS.forEach(([a, b]) => {
            const pts = [
                new THREE.Vector3(...NODE_DATA[a].pos),
                new THREE.Vector3(...NODE_DATA[b].pos),
            ];
            const geo = new THREE.BufferGeometry().setFromPoints(pts);
            const mat = new THREE.LineBasicMaterial({
                color: 0x3B82F6,
                transparent: true,
                opacity: 0.22,
                depthWrite: false,
                blending: THREE.AdditiveBlending,
            });
            const line = new THREE.Line(geo, mat);
            scene.add(line);
            edgeMeshes.push({ line, mat, a, b });
        });

        // ── Animated Data Packets ─────────────────────────────────────────
        const packetGeo = new THREE.SphereGeometry(0.09, 10, 10);
        const packets   = [];

        EDGE_PAIRS.forEach(([a, b], i) => {
            // 2 packets per edge traveling in opposite directions
            [0, 1].forEach(dir => {
                const col = dir === 0 ? NODE_DATA[a].color : NODE_DATA[b].color;
                const mat = new THREE.MeshBasicMaterial({
                    color: col,
                    transparent: true,
                    opacity: 0.9,
                    depthWrite: false,
                    blending: THREE.AdditiveBlending,
                });
                const mesh = new THREE.Mesh(packetGeo, mat);
                mesh.userData = {
                    a, b,
                    t: Math.random(),
                    speed: 0.003 + Math.random() * 0.004,
                    dir: dir === 0 ? 1 : -1,
                };
                scene.add(mesh);
                packets.push(mesh);
            });
        });

        // ── Background Particles ──────────────────────────────────────────
        const bgPCount = IS_MOBILE ? 400 : 800;
        const bgPos    = new Float32Array(bgPCount * 3);
        for (let i = 0; i < bgPCount * 3; i++) bgPos[i] = (Math.random()-0.5)*18;
        const bgGeo = new THREE.BufferGeometry();
        bgGeo.setAttribute('position', new THREE.BufferAttribute(bgPos, 3));
        scene.add(new THREE.Points(bgGeo, new THREE.PointsMaterial({
            color: 0x3B82F6, size: 0.025, transparent: true, opacity: 0.28,
            depthWrite: false, blending: THREE.AdditiveBlending,
        })));

        // ── Mouse Orbit ────────────────────────────────────────────────────
        let isDragging = false, lastMouse = { x: 0, y: 0 };
        let orbitX = 0, orbitY = 0, targetX = 0, targetY = 0;

        const onMousedown = e => { isDragging = true; lastMouse = { x: e.clientX, y: e.clientY }; };
        const onMouseup   = ()  => { isDragging = false; };
        const onMousemove = e  => {
            if (!isDragging) return;
            targetX += (e.clientY - lastMouse.y) * 0.005;
            targetY += (e.clientX - lastMouse.x) * 0.005;
            lastMouse = { x: e.clientX, y: e.clientY };
        };
        // Touch support
        const onTouchstart = e => { isDragging = true; lastMouse = { x: e.touches[0].clientX, y: e.touches[0].clientY }; };
        const onTouchmove  = e => {
            if (!isDragging) return;
            targetX += (e.touches[0].clientY - lastMouse.y) * 0.005;
            targetY += (e.touches[0].clientX - lastMouse.x) * 0.005;
            lastMouse = { x: e.touches[0].clientX, y: e.touches[0].clientY };
        };

        canvas.addEventListener('mousedown', onMousedown);
        window.addEventListener('mouseup', onMouseup);
        window.addEventListener('mousemove', onMousemove);
        canvas.addEventListener('touchstart', onTouchstart, { passive: true });
        window.addEventListener('touchend', onMouseup);
        window.addEventListener('touchmove', onTouchmove, { passive: true });

        // ── Raycaster for Node Click ──────────────────────────────────────
        const raycaster = new THREE.Raycaster();
        const mouse2d   = new THREE.Vector2();
        const onCanvasClick = e => {
            const rect = canvas.getBoundingClientRect();
            mouse2d.x =  ((e.clientX - rect.left) / rect.width)  * 2 - 1;
            mouse2d.y = -((e.clientY - rect.top)  / rect.height) * 2 + 1;
            raycaster.setFromCamera(mouse2d, camera);
            const hits = raycaster.intersectObjects(nodeMeshes);
            if (hits.length > 0) {
                const idx = hits[0].object.userData.index;
                if (typeof window.selectNode === 'function') window.selectNode(idx);
            }
        };
        canvas.addEventListener('click', onCanvasClick);

        // Expose select for external calls
        window._selectedNode = 0;
        window.selectNodeMesh = i => {
            window._selectedNode = i;
        };

        // ── Label Projection ──────────────────────────────────────────────
        const _v = new THREE.Vector3();
        const _a = new THREE.Vector3();
        const _b = new THREE.Vector3();

        function updateLabels() {
            nodeMeshes.forEach((mesh, i) => {
                if (!nodeLabels[i]) return;
                const pos = _v.copy(mesh.position).project(camera);
                const x = (pos.x + 1) / 2 * canvas.clientWidth;
                const y = (1 - pos.y) / 2 * canvas.clientHeight;
                nodeLabels[i].style.left = x + 'px';
                nodeLabels[i].style.top  = y + 'px';
            });
        }

        // ── Animation Loop ────────────────────────────────────────────────
        const clock = new THREE.Clock();
        let raf;

        function animate() {
            raf = requestAnimationFrame(animate);
            if (document.hidden) return;

            const t = clock.getElapsedTime();

            // Inertia orbit
            orbitX += (targetX - orbitX) * 0.08;
            orbitY += (targetY - orbitY) * 0.08;
            if (!isDragging) targetY += 0.003;

            scene.rotation.y = orbitY;
            scene.rotation.x = Math.max(-0.6, Math.min(0.6, orbitX));

            // Pulse nodes
            nodeMeshes.forEach((mesh, i) => {
                const selected = i === (window._selectedNode || 0);
                const pulse = 1 + Math.sin(t * 1.6 + i * 1.3) * 0.065;
                mesh.scale.setScalar(selected ? pulse * 1.28 : pulse);
                mesh.material.emissiveIntensity = selected
                    ? 1.1 + Math.sin(t * 3) * 0.35
                    : 0.6 + Math.sin(t * 1.5 + i) * 0.2;

                if (mesh.userData.wire) {
                    mesh.userData.wire.rotation.y = t * 0.3 + i;
                }
                if (mesh.userData.rings) {
                    mesh.userData.rings.forEach((ring, ri) => {
                        ring.rotation.z = t * (0.4 + ri * 0.2) * (ri % 2 === 0 ? 1 : -1);
                        ring.material.opacity = (0.15 - ri * 0.04) * (0.6 + Math.sin(t * 2 + i + ri) * 0.4);
                    });
                }
            });

            // Edge opacity pulse
            edgeMeshes.forEach(({ mat }, i) => {
                mat.opacity = 0.12 + Math.sin(t * 0.9 + i * 1.1) * 0.1;
            });

            // Move packets along edges
            packets.forEach(p => {
                p.userData.t += p.userData.speed * p.userData.dir;
                if (p.userData.t >= 1) { p.userData.t = 1; p.userData.dir = -1; }
                if (p.userData.t <= 0) { p.userData.t = 0; p.userData.dir =  1; }
                const { a, b } = p.userData;
                _a.set(...NODE_DATA[a].pos);
                _b.set(...NODE_DATA[b].pos);
                p.position.lerpVectors(_a, _b, p.userData.t);
                p.material.opacity = 0.5 + Math.sin(t * 5 + p.userData.t * 8) * 0.45;
                const s = 0.8 + Math.sin(t * 4 + p.userData.t * 6) * 0.2;
                p.scale.setScalar(s);
            });

            updateLabels();
            renderer.render(scene, camera);
        }

        animate();

        const onVisibility = () => {
            if (document.hidden) cancelAnimationFrame(raf);
            else animate();
        };
        document.addEventListener('visibilitychange', onVisibility);
        canvas.addEventListener('webglcontextlost', e => { e.preventDefault(); cancelAnimationFrame(raf); });
        canvas.addEventListener('webglcontextrestored', () => { animate(); });

        return {
            destroy() {
                cancelAnimationFrame(raf);
                stopResize();
                canvas.removeEventListener('mousedown', onMousedown);
                window.removeEventListener('mouseup', onMouseup);
                window.removeEventListener('mousemove', onMousemove);
                canvas.removeEventListener('click', onCanvasClick);
                document.removeEventListener('visibilitychange', onVisibility);
                renderer.dispose();
            }
        };
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // C. DASHBOARD HERO BANNER — Torus Knot + Particles
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    function DashboardHeroBanner(canvasId) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return { destroy: () => {} };

        const renderer = setupRenderer(canvas, 0x080E1C, false);
        const scene    = new THREE.Scene();

        const camera = new THREE.PerspectiveCamera(50, 1, 0.1, 60);
        camera.position.set(0, 0, 6);

        const stopResize = makeResizer(renderer, camera, canvas);

        // Lights
        scene.add(new THREE.AmbientLight(0x3B82F6, 0.2));
        const pl1 = new THREE.PointLight(0x3B82F6, 6, 20);
        pl1.position.set(4, 3, 4);
        scene.add(pl1);
        const pl2 = new THREE.PointLight(0x8B5CF6, 4, 16);
        pl2.position.set(-4, -2, 2);
        scene.add(pl2);
        const pl3 = new THREE.PointLight(0x22D3EE, 3, 12);
        pl3.position.set(0, 4, -3);
        scene.add(pl3);

        // Main torus knot
        const tkGeo = new THREE.TorusKnotGeometry(1.1, 0.28, 180, 20, 2, 3);
        const tkMat = new THREE.MeshStandardMaterial({
            color: 0x3B82F6,
            emissive: 0x1d4ed8,
            emissiveIntensity: 0.5,
            metalness: 0.8,
            roughness: 0.15,
            wireframe: false,
        });
        const torusKnot = new THREE.Mesh(tkGeo, tkMat);
        torusKnot.position.set(3.5, 0, 0);
        scene.add(torusKnot);

        // Wireframe overlay on torus
        const tkWireMat = new THREE.MeshBasicMaterial({
            color: 0x60A5FA,
            wireframe: true,
            transparent: true,
            opacity: 0.06,
        });
        const tkWire = new THREE.Mesh(tkGeo, tkWireMat);
        tkWire.position.copy(torusKnot.position);
        scene.add(tkWire);

        // Orbiting icosahedra
        const orbitGeo = new THREE.IcosahedronGeometry(0.18, 1);
        const orbiters = [];
        [0x60A5FA, 0xA78BFA, 0x34D399, 0xFBBF24].forEach((col, i) => {
            const mat = new THREE.MeshStandardMaterial({
                color: col,
                emissive: col,
                emissiveIntensity: 0.8,
                metalness: 0.6,
                roughness: 0.3,
            });
            const mesh = new THREE.Mesh(orbitGeo, mat);
            mesh.userData = { orbitR: 1.4 + i * 0.25, orbitSpeed: 0.4 + i * 0.15, orbitOffset: i * (Math.PI / 2) };
            scene.add(mesh);
            orbiters.push(mesh);
        });

        // Particle field
        const pCount = IS_MOBILE ? 300 : 700;
        const pPos   = new Float32Array(pCount * 3);
        for (let i = 0; i < pCount * 3; i++) pPos[i] = (Math.random()-0.5) * 12;
        const pGeo = new THREE.BufferGeometry();
        pGeo.setAttribute('position', new THREE.BufferAttribute(pPos, 3));
        const pMesh = new THREE.Points(pGeo, new THREE.PointsMaterial({
            color: 0x3B82F6, size: 0.03, transparent: true, opacity: 0.35,
            depthWrite: false, blending: THREE.AdditiveBlending,
        }));
        scene.add(pMesh);

        // Mouse parallax
        let mx = 0, my = 0, tx = 0, ty = 0;
        const onMM = e => {
            tx = (e.clientX / window.innerWidth  - 0.5) * 1.5;
            ty = (e.clientY / window.innerHeight - 0.5) * 0.8;
        };
        document.addEventListener('mousemove', onMM);

        const clock = new THREE.Clock();
        let raf;

        function animate() {
            raf = requestAnimationFrame(animate);
            if (document.hidden) return;
            const t = clock.getElapsedTime();

            mx = lerp(mx, tx, 0.05);
            my = lerp(my, ty, 0.05);

            torusKnot.rotation.x = t * 0.3;
            torusKnot.rotation.y = t * 0.2;
            tkWire.rotation.copy(torusKnot.rotation);

            // Color cycling on torus material
            const hue = (t * 0.03) % 1;
            tkMat.emissive.setHSL(hue, 0.8, 0.3);
            tkMat.emissiveIntensity = 0.4 + Math.sin(t * 1.5) * 0.2;

            orbiters.forEach((o, i) => {
                const ang = t * o.userData.orbitSpeed + o.userData.orbitOffset;
                o.position.set(
                    torusKnot.position.x + Math.cos(ang) * o.userData.orbitR,
                    Math.sin(ang * 0.7) * o.userData.orbitR * 0.5,
                    Math.sin(ang) * o.userData.orbitR
                );
                o.rotation.x = t * 1.2 + i;
                o.rotation.y = t * 0.8 + i;
            });

            pMesh.rotation.y = t * 0.012;

            camera.position.x = lerp(camera.position.x, mx * 0.6, 0.03);
            camera.position.y = lerp(camera.position.y, -my * 0.4, 0.03);
            camera.lookAt(0, 0, 0);

            renderer.render(scene, camera);
        }

        animate();
        const onVis = () => { if (document.hidden) cancelAnimationFrame(raf); else animate(); };
        document.addEventListener('visibilitychange', onVis);
        canvas.addEventListener('webglcontextlost', e => { e.preventDefault(); cancelAnimationFrame(raf); });
        canvas.addEventListener('webglcontextrestored', () => { animate(); });

        return {
            destroy() {
                cancelAnimationFrame(raf);
                stopResize();
                document.removeEventListener('mousemove', onMM);
                document.removeEventListener('visibilitychange', onVis);
                renderer.dispose();
            }
        };
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // D. GLOBAL PARTICLE BACKGROUND — Ultra-lightweight ambient field
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    function GlobalParticleBackground(canvasId) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return { destroy: () => {} };

        const renderer = setupRenderer(canvas, 0x080E1C, true);
        renderer.setClearAlpha(0);

        const scene  = new THREE.Scene();
        const camera = new THREE.PerspectiveCamera(70, 1, 0.1, 100);
        camera.position.z = 6;

        const stopResize = makeResizer(renderer, camera, canvas);

        const pCount = IS_MOBILE ? 250 : 600;
        const pos    = new Float32Array(pCount * 3);
        const vel    = new Float32Array(pCount * 3);

        for (let i = 0; i < pCount; i++) {
            pos[i*3]   = (Math.random()-0.5)*20;
            pos[i*3+1] = (Math.random()-0.5)*12;
            pos[i*3+2] = (Math.random()-0.5)*8 - 4;
            vel[i*3]   = (Math.random()-0.5)*0.003;
            vel[i*3+1] = (Math.random()-0.5)*0.002 - 0.001;
            vel[i*3+2] = 0;
        }

        const geo = new THREE.BufferGeometry();
        const posAttr = new THREE.BufferAttribute(pos, 3);
        posAttr.setUsage(THREE.DynamicDrawUsage);
        geo.setAttribute('position', posAttr);

        const mat = new THREE.PointsMaterial({
            color: 0x3B82F6,
            size: 0.045,
            transparent: true,
            opacity: 0.18,
            depthWrite: false,
            blending: THREE.AdditiveBlending,
        });
        const points = new THREE.Points(geo, mat);
        scene.add(points);

        // Mouse subtle push
        let mx = 0, my = 0;
        const onMM = e => {
            mx = (e.clientX / window.innerWidth  - 0.5) * 0.002;
            my = (e.clientY / window.innerHeight - 0.5) * 0.001;
        };
        document.addEventListener('mousemove', onMM);

        let raf;
        function animate() {
            raf = requestAnimationFrame(animate);
            if (document.hidden) return;

            for (let i = 0; i < pCount; i++) {
                pos[i*3]   += vel[i*3]   + mx;
                pos[i*3+1] += vel[i*3+1] - my;

                if (pos[i*3]   >  11) pos[i*3]   = -11;
                if (pos[i*3]   < -11) pos[i*3]   =  11;
                if (pos[i*3+1] >   7) pos[i*3+1] =  -7;
                if (pos[i*3+1] <  -7) pos[i*3+1] =   7;
            }
            posAttr.needsUpdate = true;

            renderer.render(scene, camera);
        }

        animate();
        const onVis = () => { if (document.hidden) cancelAnimationFrame(raf); else animate(); };
        document.addEventListener('visibilitychange', onVis);
        canvas.addEventListener('webglcontextlost', e => { e.preventDefault(); cancelAnimationFrame(raf); });
        canvas.addEventListener('webglcontextrestored', () => { animate(); });

        return {
            destroy() {
                cancelAnimationFrame(raf);
                stopResize();
                document.removeEventListener('mousemove', onMM);
                document.removeEventListener('visibilitychange', onVis);
                renderer.dispose();
            }
        };
    }

    // Public API
    return { HeroAuthScene, NodeTopologyScene, DashboardHeroBanner, GlobalParticleBackground };

})();
