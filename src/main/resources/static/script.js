const PassportOCRApp = {
    elements: {
        video: document.getElementById("camera-preview") || document.getElementById("camera"),
        captureBtn: document.getElementById("captureBtn"),
        nextBtn: document.getElementById("nextBtn"),
        container: document.querySelector(".container"),
        retakeBtn: document.getElementById("retakeBtn"),
        continueCameraBtn: document.getElementById("continueCameraBtn"),
        phoneInput: document.getElementById("phone"),
        emailInput: document.getElementById("email")
    },

    state: {
        photoTaken: false,
        capturedData: "",
        phone: "",
        email: ""
    },

    config: {
        BACKEND_URL: "/api/ocr/passport"
    },

    methods: {
        startCamera: async function () {
            const video = PassportOCRApp.elements.video;
            if (!video) return;
            try {
                const stream = await navigator.mediaDevices.getUserMedia({
                    video: { facingMode: { ideal: "environment" } }
                });
                video.srcObject = stream;
            } catch (err) {
                alert("Camera access denied or unavailable.");
                console.error(err);
            }
        },

        capturePhoto: function () {
            const video = PassportOCRApp.elements.video;
            if (!video) return;

            const canvas = document.createElement("canvas");
            const ctx = canvas.getContext("2d");

            // Get crop bounds from scan-frame overlay (scales to video's natural resolution)
            const frame = document.getElementById('scanFrame');
            let cropX = 0, cropY = 0, cropW = video.videoWidth, cropH = video.videoHeight;

            if (frame) {
                const videoRect = video.getBoundingClientRect();
                const frameRect = frame.getBoundingClientRect();
                const scaleX = video.videoWidth / videoRect.width;
                const scaleY = video.videoHeight / videoRect.height;

                cropX = (frameRect.left - videoRect.left) * scaleX;
                cropY = (frameRect.top - videoRect.top) * scaleY;
                cropW = frameRect.width * scaleX;
                cropH = frameRect.height * scaleY;
            }

            // Set canvas to crop size and draw the cropped area
            canvas.width = cropW;
            canvas.height = cropH;
            ctx.drawImage(video, cropX, cropY, cropW, cropH, 0, 0, cropW, cropH);

            PassportOCRApp.state.capturedData = canvas.toDataURL("image/jpeg", 0.9);  // Higher quality JPEG

            // Show preview (unchanged)
            let img = document.getElementById("captured-img");
            const container = PassportOCRApp.elements.container;
            if (!img && container) {
                img = document.createElement("img");
                img.id = "captured-img";
                img.style.display = "block";
                img.style.margin = "12px auto";
                img.style.maxWidth = "320px";
                img.style.border = "2px solid #004aad";
                img.style.borderRadius = "8px";
                container.appendChild(img);
            }
            if (img) img.src = PassportOCRApp.state.capturedData;

            PassportOCRApp.state.photoTaken = true;
            if (PassportOCRApp.elements.nextBtn) PassportOCRApp.elements.nextBtn.disabled = false;
        },

        dataURLtoBlob: function (dataURL) {
            const parts = dataURL.split(",");
            const mime = parts[0].match(/:(.*?);/)[1];
            const binary = atob(parts[1]);
            const array = new Uint8Array(binary.length);
            for (let i = 0; i < binary.length; i++) array[i] = binary.charCodeAt(i);
            return new Blob([array], { type: mime });
        },

        sendImageToBackend: async function () {
            if (!PassportOCRApp.state.photoTaken) {
                alert("Please capture the passport image first.");
                return;
            }

            // Grab phone and email from index.html inputs
            PassportOCRApp.state.phone = PassportOCRApp.elements.phoneInput?.value || "";
            PassportOCRApp.state.email = PassportOCRApp.elements.emailInput?.value || "";

            const imageBlob = PassportOCRApp.methods.dataURLtoBlob(PassportOCRApp.state.capturedData);
            const imageFile = new File([imageBlob], "passport.jpg", { type: "image/jpeg" });
            const formData = new FormData();
            formData.append("image", imageFile);

            try {
                const response = await fetch(PassportOCRApp.config.BACKEND_URL, {
                    method: "POST",
                    body: formData
                });

                if (!response.ok) {
                    const text = await response.text();
                    throw new Error(text);
                }

                const ocrResult = await response.json();
                // Debug: see what came back from backend
                console.log("Backend OCR response:", ocrResult);
                console.log("Raw OCR text:", ocrResult.rawText);

                // Save OCR result + image + phone/email
                sessionStorage.setItem("ocrData", JSON.stringify(ocrResult));
                sessionStorage.setItem("passportImage", PassportOCRApp.state.capturedData);
                sessionStorage.setItem("phone", PassportOCRApp.state.phone);
                sessionStorage.setItem("email", PassportOCRApp.state.email);

                // Redirect to verification page
                window.location.href = "verify.html";
            } catch (err) {
                console.error(err);
                alert("Failed to connect to backend OCR service.");
            }
        },

        retakePhoto: function () {
            PassportOCRApp.state.photoTaken = false;
            PassportOCRApp.state.capturedData = "";
            if (PassportOCRApp.elements.nextBtn) PassportOCRApp.elements.nextBtn.disabled = true;
            PassportOCRApp.methods.startCamera();
        },

        prefillVerifyPage: function () {
            const raw = sessionStorage.getItem("ocrData");
            if (!raw) return;
            const ocrData = JSON.parse(raw);

            const fields = [
                "firstName",
                "lastName",
                "passportNumber",
                "nationality",
                "dateOfBirth",
                "gender",
                "expiryDate"
            ];
            fields.forEach((id) => {
                const el = document.getElementById(id);
                if (el && ocrData[id]) el.value = ocrData[id];
            });

            // Prefill phone and email
            const phoneEl = document.getElementById("phone");
            const emailEl = document.getElementById("email");
            if (phoneEl) phoneEl.value = sessionStorage.getItem("phone") || "";
            if (emailEl) emailEl.value = sessionStorage.getItem("email") || "";
        }
    },

    events: function () {
        if (PassportOCRApp.elements.captureBtn)
            PassportOCRApp.elements.captureBtn.addEventListener("click", PassportOCRApp.methods.capturePhoto);

        if (PassportOCRApp.elements.nextBtn) {
            PassportOCRApp.elements.nextBtn.disabled = true;
            PassportOCRApp.elements.nextBtn.addEventListener("click", PassportOCRApp.methods.sendImageToBackend);
        }

        if (PassportOCRApp.elements.retakeBtn)
            PassportOCRApp.elements.retakeBtn.addEventListener("click", PassportOCRApp.methods.retakePhoto);

        if (PassportOCRApp.elements.continueCameraBtn)
            PassportOCRApp.elements.continueCameraBtn.addEventListener("click", () => {
                alert("Continue button not implemented yet.");
            });
    },

    init: function () {
        if (PassportOCRApp.elements.video) PassportOCRApp.methods.startCamera();
        PassportOCRApp.events();
        PassportOCRApp.methods.prefillVerifyPage();
    }
};

// Run the app
PassportOCRApp.init();
