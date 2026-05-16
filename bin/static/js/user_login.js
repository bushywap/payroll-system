document.addEventListener('DOMContentLoaded', () => {
    // --- 1. Element Selection ---
    const togglePassBtn = document.getElementById('togglePassBtn');
    const passwordInput = document.getElementById('password');
    const loginForm = document.getElementById('loginForm');
    const loginBtn = document.getElementById('loginBtn');
    const toastContainer = document.getElementById('toastContainer');
    
    // Forgot Password Modal Elements
    const forgotLink = document.getElementById('forgotLink');
    const modalOverlay = document.getElementById('modalOverlay');
    const cancelModalBtn = document.getElementById('cancelModalBtn');
    const resetBtn = document.getElementById('resetBtn');
    const resetEmail = document.getElementById('resetEmail');

    if (!loginForm || !loginBtn) {
        console.error("Critical login form elements are missing.");
        return;
    }
    
    // --- 2. Utility Functions ---
    function showToast(type, title, message){
        if (!toastContainer) return; 
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        const icon = type === 'success' ? 'fa-circle-check' : 'fa-circle-exclamation';
        toast.innerHTML = `
            <i class="fa-solid ${icon}"></i>
            <div><strong>${title}</strong><div style="font-size:13px;color:inherit">${message}</div></div>
        `;
        toastContainer.appendChild(toast);
        setTimeout(() => toast.classList.add('show'), 10);
        setTimeout(() => {
            toast.classList.remove('show'); 
            setTimeout(() => toast.remove(), 400);
        }, 3500);
    }

    function setLoading(btn, loading){
        const txt = btn.querySelector('.btn-text');
        const loader = btn.querySelector('.loader');
        if (!btn || !txt || !loader) return; 
        btn.disabled = loading;
        txt.style.display = loading ? 'none':'inline';
        loader.style.display = loading ? 'block':'none';
    }
    
    // --- 3. Event Listeners ---
    if (togglePassBtn && passwordInput) {
        togglePassBtn.addEventListener('click', () => {
            passwordInput.type = passwordInput.type === "password" ? "text" : "password";
            const iconElement = togglePassBtn.querySelector('i');
            if (iconElement) {
                iconElement.classList.toggle('fa-eye');
                iconElement.classList.toggle('fa-eye-slash');
            }
        });
    }

    // --- 4. Login Submission (AJAX) ---
    loginForm.addEventListener('submit', async e => {
        e.preventDefault(); 

        const emailElement = document.getElementById('email');
        const email = emailElement ? emailElement.value.trim() : '';
        const password = passwordInput ? passwordInput.value : '';

        if(!email || !password) return showToast('error','Missing fields','Please fill all fields.');

        setLoading(loginBtn, true);

        try{
            const response = await fetch('/api/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: email, password: password }),
                credentials: 'include' 
            });

            if (!response.ok && response.status !== 401) {
                 throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();

            if (data.success) {
                 showToast('success','Welcome', data.message);
                 
                 // REDIRECT: Go to payroll dashboard after login
                 setTimeout(() => {
                     location.href = '/payroll'; 
                 }, 900);
            } else {
                 showToast('error','Authentication Failed', data.message);
            }

        } catch(err){
            console.error('Login error:', err);
            showToast('error','Server Error','Could not connect to the server.');
        } finally {
            setLoading(loginBtn, false);
        }
    });

    // --- 5. Forgot Password Modal Logic ---
    if (forgotLink && modalOverlay && cancelModalBtn && resetBtn && resetEmail) {
        forgotLink.addEventListener('click', e => {
            e.preventDefault(); 
            modalOverlay.style.display = 'flex';
        });
        
        cancelModalBtn.addEventListener('click', () => modalOverlay.style.display = 'none');

        resetBtn.addEventListener('click', async e => {
            e.preventDefault(); 
            const email = resetEmail.value.trim();
            if (!email) return showToast('error','Required','Please enter your email.');
            setLoading(resetBtn, true);
            try {
                const response = await fetch('/api/forgot-password', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email: email })
                });
                const data = await response.json();
                if (data.success) {
                     showToast('success','Link Sent', data.message);
                     resetEmail.value = '';
                     setTimeout(() => modalOverlay.style.display = 'none', 500); 
                } else {
                     showToast('error','Request Failed', data.message);
                }
            } catch (err) {
                console.error('Forgot password error:', err);
                showToast('error','Server Error','Failed to process request.');
            } finally {
                setLoading(resetBtn, false);
            }
        });
    }
});