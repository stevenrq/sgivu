const ValidationRules = {
  username: [
    (value) => {
      const v = value.trim();
      return v
        ? { valid: true }
        : {
            valid: false,
            message: "El nombre de usuario es obligatorio.",
          };
    },
  ],

  password: [
    (value) => {
      return value
        ? { valid: true }
        : {
            valid: false,
            message: "La contraseña es obligatoria.",
          };
    },
  ],
};

const ErrorMessageMap = {
  invalid_credentials:
    "Credenciales inválidas. Verifique su nombre de usuario y contraseña.",
  disabled: "Su cuenta está deshabilitada. Contacte al administrador.",
  locked: "Su cuenta está temporalmente bloqueada. Inténtelo más tarde.",
  expired: "Su cuenta ha expirado. Contacte al administrador.",
  credentials: "Sus credenciales han expirado. Cambie su contraseña.",
  service_unavailable:
    "El servicio no está disponible actualmente. Inténtelo más tarde.",
  unexpected_error:
    "Ocurrió un error inesperado. Por favor, inténtelo de nuevo.",
};

class InputValidator {
  constructor(input, errorSpan, rules) {
    this.input = input;
    this.errorSpan = errorSpan;
    this.rules = rules;

    this.touched = false;
  }

  validate() {
    const value = this.input.value;

    for (const rule of this.rules) {
      const result = rule(value);
      if (!result.valid) {
        this.setBootstrapValidation(false, result.message);
        return false;
      }
    }

    this.setBootstrapValidation(true, "");
    return true;
  }

  setBootstrapValidation(isValid, message) {
    if (!this.touched) {
      this.clearValidation();
      return;
    }

    this.input.classList.remove("is-invalid", "is-valid");
    this.input.classList.add(isValid ? "is-valid" : "is-invalid");

    this.errorSpan.textContent = isValid ? "" : message;
    this.errorSpan.classList.toggle("d-block", !isValid);
  }

  clearValidation() {
    this.input.classList.remove("is-invalid", "is-valid");
    this.errorSpan.textContent = "";
    this.errorSpan.classList.remove("d-block");
  }
}

class LoginFormHandler {
  constructor() {
    this.initializeElements();
    this.initializeValidators();
    this.attachEventListeners();
  }

  initializeElements() {
    this.loginForm = document.querySelector("form");
    this.usernameInput = document.getElementById("username");
    this.passwordInput = document.getElementById("password");
    this.usernameError = document.getElementById("username-error-js");
    this.passwordError = document.getElementById("password-error-js");
    this.togglePassword = document.getElementById("toggle-password");
  }

  initializeValidators() {
    if (this.usernameInput && this.usernameError) {
      this.usernameValidator = new InputValidator(
        this.usernameInput,
        this.usernameError,
        ValidationRules.username
      );
    }

    if (this.passwordInput && this.passwordError) {
      this.passwordValidator = new InputValidator(
        this.passwordInput,
        this.passwordError,
        ValidationRules.password
      );
    }
  }

  attachEventListeners() {
    if (this.loginForm) {
      this.loginForm.addEventListener("submit", (event) =>
        this.handleFormSubmit(event)
      );
    }

    if (this.togglePassword) {
      this.togglePassword.addEventListener("click", () =>
        this.togglePasswordVisibility()
      );
    }
  }

  async handleFormSubmit(event) {
    event.preventDefault();

    this.usernameValidator.touched = true;
    this.passwordValidator.touched = true;

    const isUsernameValid = this.usernameValidator.validate();
    const isPasswordValid = this.passwordValidator.validate();

    if (!isUsernameValid || !isPasswordValid) {
      return;
    }

    const credentialsValid = await this.validateCredentials();
    if (credentialsValid) {
      this.loginForm.submit();
    }
  }

  togglePasswordVisibility() {
    const isPassword = this.passwordInput.type === "password";
    this.passwordInput.type = isPassword ? "text" : "password";
    this.togglePassword.classList.toggle("bi-eye");
    this.togglePassword.classList.toggle("bi-eye-slash");
  }

  async validateCredentials() {
    const username = this.usernameInput.value.trim();
    const password = this.passwordInput.value;

    if (!username || !password) {
      return false;
    }

    const payload = {
      username: username,
      password: password,
    };

    try {
      const response = await fetch("/api/validate-credentials", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      return this.handleCredentialsValidationResponse(data);
    } catch (error) {
      console.error("Error validating credentials:", error);
      // En caso de error de red, permitir que el servidor lo maneje
      return true;
    }
  }

  handleCredentialsValidationResponse(response) {
    if (response.valid) {
      this.usernameValidator.setBootstrapValidation(true, "");
      this.passwordValidator.setBootstrapValidation(true, "");
      return true;
    } else {
      const errorMessage =
        ErrorMessageMap[response.reason] || ErrorMessageMap.unexpected_error;
      this.usernameValidator.setBootstrapValidation(false, "");
      if (this.usernameError) {
        this.usernameError.textContent = "";
        this.usernameError.classList.remove("d-block");
      }
      this.passwordValidator.setBootstrapValidation(false, errorMessage);
      return false;
    }
  }
}

document.addEventListener("DOMContentLoaded", () => {
  new LoginFormHandler();
});
