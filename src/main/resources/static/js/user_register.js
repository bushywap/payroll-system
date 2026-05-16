document.addEventListener('DOMContentLoaded', () => {
    
    // --- Card Reveal Animation (Moved from HTML) ---
    const registerCard = document.querySelector('.login-card');
    if (registerCard) {
        registerCard.style.display = 'block'; 
        registerCard.style.animation = 'loginCardReveal 0.8s ease-out forwards';
    }

    // --- Form Handling Logic ---
    const form = document.getElementById('registerForm');
    const btn = document.getElementById('regBtn');

    form.addEventListener('submit', (e) => {
        e.preventDefault();
        
        const firstName = document.getElementById("regFirstName").value.trim();
        const lastName = document.getElementById("regLastName").value.trim();
        const email = document.getElementById("regEmail").value.trim();
        const password = document.getElementById("regPassword").value;
        const confirmPass = document.getElementById("confirmPassword").value;

        // Validation
        if(!firstName || !lastName || !email || !password || !confirmPass) {
            showToast("error", "Missing Info", "Please fill in all fields.");
            return;
        }

        if(password !== confirmPass) {
            showToast("error", "Error", "Passwords do not match.");
            return;
        }

        // Loading
        btn.disabled = true;
        btn.querySelector(".btn-text").style.display = "none";
        btn.querySelector(".loader").style.display = "block";

        // Fetch API Call
        fetch('/payroll-system/api/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password, firstName, lastName })
        })
        .then(response => response.json())
        .then(data => {
            btn.querySelector(".loader").style.display = "none";
            btn.querySelector(".btn-text").style.display = "block";
            
            if(data.success) {
                showToast("success", "Email Sent", data.message);
                btn.disabled = true; 
                btn.querySelector(".btn-text").innerText = "Check Your Email";
            } else {
                btn.disabled = false;
                showToast("error", "Failed", data.message);
            }
        })
        .catch(() => {
            btn.disabled = false;
            btn.querySelector(".loader").style.display = "none";
            btn.querySelector(".btn-text").style.display = "block";
            showToast("error", "Error", "Server connection failed.");
        });
    });

    function showToast(type, title, message) {
        const container = document.getElementById("toastContainer");
        const toast = document.createElement("div");
        toast.className = `toast ${type}`;
        toast.innerHTML = `
            <i class="fa-solid ${type==='success'?'fa-circle-check':'fa-circle-exclamation'}"></i>
            <div><strong>${title}</strong><br><span style="font-size:13px;">${message}</span></div>
        `;
        container.appendChild(toast);
        setTimeout(() => toast.classList.add("show"), 10);
        setTimeout(() => { toast.classList.remove("show"); setTimeout(() => toast.remove(), 400); }, 3000);
    }
});