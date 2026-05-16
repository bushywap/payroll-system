document.addEventListener('DOMContentLoaded', () => {
    
    const saveBtn = document.getElementById("saveBtn");
    
    saveBtn.addEventListener("click", () => {
        const token = document.getElementById("token").value;
        const p1 = document.getElementById("newPass").value;
        const p2 = document.getElementById("confirmPass").value;

        // 1. Validation
        if(!p1 || !p2) { 
            showToast("error", "Missing Info", "Please fill in all fields."); 
            return; 
        }
        
        if(p1 !== p2) { 
            showToast("error", " mismatch", "Passwords do not match."); 
            return; 
        }

        // 2. Loading State
        setLoading(true);

        // 3. API Call
        fetch('/api/reset-password', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ token: token, password: p1 })
        })
        .then(res => res.json())
        .then(data => {
            setLoading(false);

            if(data.success) {
                showToast("success", "Success", "Password updated! Redirecting...");
                // Redirect to login after 2 seconds
                setTimeout(() => window.location.href = "/login", 2000);
            } else {
                showToast("error", "Error", data.message);
            }
        })
        .catch(() => {
            setLoading(false);
            showToast("error", "System Error", "Could not connect to server.");
        });
    });

    // --- Helper Functions ---

    function setLoading(isLoading) {
        saveBtn.disabled = isLoading;
        saveBtn.querySelector(".btn-text").style.display = isLoading ? "none" : "block";
        saveBtn.querySelector(".loader").style.display = isLoading ? "block" : "none";
    }

    function showToast(type, title, message) {
        const container = document.getElementById("toastContainer");
        const toast = document.createElement("div");
        toast.className = `toast ${type}`;
        
        const iconClass = type === 'success' ? 'fa-circle-check' : 'fa-circle-exclamation';
        
        toast.innerHTML = `
            <i class="fa-solid ${iconClass}"></i>
            <div><strong>${title}</strong><br><span style="font-size:13px;">${message}</span></div>
        `;
        
        container.appendChild(toast);
        setTimeout(() => toast.classList.add("show"), 10);
        setTimeout(() => { toast.classList.remove("show"); setTimeout(() => toast.remove(), 400); }, 3000);
    }
});