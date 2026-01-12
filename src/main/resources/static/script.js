// ===================================
// Unified Passport OCR Script
// Frontend in Spring Boot static resources
// ===================================

const PassportOCRApp = {
  // ------------------------------
  // Elements
  // ------------------------------
  elements: {
    video: document.getElementById("camera-preview") || document.getElementById("camera"),
    captureBtn: document.getElementById("captureBtn"),
    nextBtn: document.getElementById("nextBtn"),
    container: document.querySelector(".container"),
    retakeBtn: document.getElementById("retakeBtn"),
    continueCameraBtn: document.getElementById("continueCameraBtn")
  },

  // ------------------------------
  // State
  // ------------------------------
  state: {
    photoTaken: false,
    capturedData: ""
  },

  // ------------------------------
  // Configuration
  // ------------------------------
  config: {
    BACKEND_URL: "/api/ocr/passport"
    // replace <YOUR-PC-LAN-IP> with your PC's IP from `ipconfig` (like 192.168.x.x)
  },

  // ------------------------------
  // Methods
  // ------------------------------
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
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      canvas.getContext("2d").drawImage(video, 0, 0);
      PassportOCRApp.state.capturedData = canvas.toDataURL("image/jpeg");

      // Show preview
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

    // Convert dataURL to Blob for real file upload
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

      const imageBlob = PassportOCRApp.methods.dataURLtoBlob(PassportOCRApp.state.capturedData);
      const imageFile = new File([imageBlob], "passport.jpg", { type: "image/jpeg" });
      const formData = new FormData();
      formData.append("image", imageFile); // key must match backend @RequestParam("image")

      try {
        const response = await fetch(PassportOCRApp.config.BACKEND_URL, {
          method: "POST",
          body: formData // no Content-Type needed, browser sets multipart automatically
        });

        if (!response.ok) {
          const text = await response.text();
          throw new Error(text);
        }

        const ocrResult = await response.json();

        // Save OCR result and image for verification page
        sessionStorage.setItem("ocrData", JSON.stringify(ocrResult));
        sessionStorage.setItem("passportImage", PassportOCRApp.state.capturedData);

        // Navigate to verification page
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
    }
  },

  // ------------------------------
  // Event bindings
  // ------------------------------
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

  // ------------------------------
  // Initialize
  // ------------------------------
  init: function () {
    if (PassportOCRApp.elements.video) PassportOCRApp.methods.startCamera();
    PassportOCRApp.events();
    PassportOCRApp.methods.prefillVerifyPage();
  }
};

// Run the app
PassportOCRApp.init();
