"use strict";

// Keep the site readable if JavaScript is disabled: only animate after successful startup.
document.documentElement.classList.add("js-ready");

const REDUCED_MOTION = window.matchMedia("(prefers-reduced-motion: reduce)");

window.addEventListener("DOMContentLoaded", () => {
  const year = document.getElementById("year");
  if (year) year.textContent = String(new Date().getFullYear());
  initNavigation();
  initReveals();
  initShowcaseDepth();
  initCertificateGallery();
  initGuestbook();
  initContact();
});

function initNavigation() {
  const header = document.querySelector(".site-header");
  const toggle = document.querySelector(".nav-toggle");
  const mobileMenu = document.querySelector(".mobile-menu");
  if (!header || !toggle || !mobileMenu) return;

  function closeMenu() {
    mobileMenu.classList.remove("open");
    toggle.setAttribute("aria-expanded", "false");
    toggle.setAttribute("aria-label", "Open navigation");
    mobileMenu.inert = true;
  }
  toggle.addEventListener("click", () => {
    const open = toggle.getAttribute("aria-expanded") !== "true";
    toggle.setAttribute("aria-expanded", String(open));
    toggle.setAttribute("aria-label", open ? "Close navigation" : "Open navigation");
    mobileMenu.classList.toggle("open", open);
    mobileMenu.inert = !open;
  });
  mobileMenu.querySelectorAll("a").forEach((link) => link.addEventListener("click", closeMenu));
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && toggle.getAttribute("aria-expanded") === "true") {
      closeMenu();
      toggle.focus();
    }
  });
  document.addEventListener("pointerdown", (event) => {
    if (!header.contains(event.target)) closeMenu();
  });
  window.matchMedia("(min-width: 901px)").addEventListener("change", (event) => {
    if (event.matches) closeMenu();
  });

  let ticking = false;
  window.addEventListener("scroll", () => {
    if (ticking) return;
    ticking = true;
    window.requestAnimationFrame(() => {
      header.classList.toggle("scrolled", window.scrollY > 12);
      ticking = false;
    });
  }, { passive: true });

  const navLinks = Array.from(document.querySelectorAll(".desktop-nav a, .mobile-menu a[href^='#']"));
  const observedSections = ["about", "expertise", "projects", "certifications", "contact"]
    .map((id) => document.getElementById(id)).filter(Boolean);
  if ("IntersectionObserver" in window) {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;
        navLinks.forEach((link) => {
          if (link.getAttribute("href") === `#${entry.target.id}`) link.setAttribute("aria-current", "location");
          else link.removeAttribute("aria-current");
        });
      });
    }, { rootMargin: "-18% 0px -68% 0px", threshold: 0 });
    observedSections.forEach((section) => observer.observe(section));
  }
}

function initReveals() {
  const elements = Array.from(document.querySelectorAll("[data-reveal]"));
  if (REDUCED_MOTION.matches || !("IntersectionObserver" in window)) {
    elements.forEach((element) => element.classList.add("is-visible"));
    return;
  }
  const observer = new IntersectionObserver((entries) => {
    for (const entry of entries) {
      if (!entry.isIntersecting) continue;
      entry.target.classList.add("is-visible");
      observer.unobserve(entry.target);
    }
  }, { threshold: .04, rootMargin: "0px 0px 35px 0px" });
  elements.forEach((element) => observer.observe(element));
}

function initShowcaseDepth() {
  const stage = document.getElementById("showcase-stage");
  if (!stage || REDUCED_MOTION.matches || !window.matchMedia("(hover: hover) and (pointer: fine)").matches) return;
  const cards = Array.from(stage.querySelectorAll("[data-depth]"));
  let frame = 0;
  function reset() {
    if (frame) cancelAnimationFrame(frame);
    for (const card of cards) {
      card.style.setProperty("--move-x", "0px");
      card.style.setProperty("--move-y", "0px");
    }
  }
  stage.addEventListener("pointermove", (event) => {
    const bounds = stage.getBoundingClientRect();
    const x = ((event.clientX - bounds.left) / bounds.width - .5) * 2;
    const y = ((event.clientY - bounds.top) / bounds.height - .5) * 2;
    if (frame) cancelAnimationFrame(frame);
    frame = requestAnimationFrame(() => {
      for (const card of cards) {
        const depth = Number(card.dataset.depth || 1);
        card.style.setProperty("--move-x", `${(x * depth * 7).toFixed(1)}px`);
        card.style.setProperty("--move-y", `${(y * depth * 5).toFixed(1)}px`);
      }
    });
  }, { passive: true });
  stage.addEventListener("pointerleave", reset);
  document.addEventListener("visibilitychange", () => {
    document.body.classList.toggle("page-hidden", document.hidden);
    if (document.hidden) reset();
  });
}

function initCertificateGallery() {
  const dialog = document.getElementById("certificate-dialog");
  const previews = document.querySelectorAll(".certificate-media[data-certificate-title]");
  if (!dialog || !previews.length || typeof dialog.showModal !== "function") return;
  const image = document.getElementById("certificate-dialog-image");
  const title = document.getElementById("certificate-dialog-title");
  const closeButton = dialog.querySelector(".certificate-dialog-close");
  if (!image || !title || !closeButton) return;
  let trigger = null;

  previews.forEach((link) => {
    link.addEventListener("click", (event) => {
      event.preventDefault();
      trigger = link;
      title.textContent = link.dataset.certificateTitle || "Certificate";
      image.alt = title.textContent;
      image.src = link.href; // Local static file; fallback link opens image without JS.
      dialog.showModal();
      closeButton.focus();
    });
  });
  closeButton.addEventListener("click", () => dialog.close());
  dialog.addEventListener("click", (event) => {
    if (event.target === dialog) dialog.close();
  });
  dialog.addEventListener("close", () => {
    image.removeAttribute("src");
    if (trigger?.isConnected) trigger.focus();
  });
}

async function requestJson(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    credentials: "same-origin",
    headers: { Accept: "application/json", ...(options.headers || {}) }
  });
  const data = await response.json().catch(() => ({}));
  return { response, data };
}

function setStatus(element, message, kind = "") {
  if (!element) return;
  element.className = `form-status ${kind}`.trim();
  element.textContent = message;
}

function initGuestbook() {
  const disclosure = document.getElementById("guestbook");
  const list = document.getElementById("comments-list");
  const form = document.getElementById("comment-form");
  const status = document.getElementById("comment-status");
  if (!disclosure || !list || !form) return;
  let loaded = false;

  function showMessage(message) {
    const paragraph = document.createElement("p");
    paragraph.className = "comment-placeholder";
    paragraph.textContent = message;
    list.replaceChildren(paragraph);
  }

  async function loadComments() {
    try {
      const { response, data } = await requestJson("/api/comments");
      if (!response.ok || !Array.isArray(data)) {
        showMessage("Comments are temporarily unavailable. Please try again later.");
        return;
      }
      if (data.length === 0) {
        showMessage("No comments yet. Be the first to leave one.");
        return;
      }
      const fragment = document.createDocumentFragment();
      for (const comment of data) {
        const article = document.createElement("article");
        article.className = "comment-item";
        const heading = document.createElement("div");
        heading.className = "comment-meta";
        const name = document.createElement("strong");
        name.textContent = String(comment.name || "Visitor");
        const dateElement = document.createElement("time");
        const date = new Date(comment.createdAt);
        if (!Number.isNaN(date.getTime())) {
          dateElement.dateTime = date.toISOString();
          dateElement.textContent = date.toLocaleDateString("en-BD", { year:"numeric",month:"short",day:"numeric" });
        }
        const text = document.createElement("p");
        text.textContent = String(comment.message || ""); // Never insert untrusted HTML.
        heading.append(name, dateElement);
        article.append(heading, text);
        fragment.appendChild(article);
      }
      list.replaceChildren(fragment);
    } catch (_) {
      showMessage("Unable to load comments right now.");
    }
  }

  disclosure.addEventListener("toggle", () => {
    if (!disclosure.open || loaded) return;
    loaded = true;
    showMessage("Loading comments…");
    loadComments();
  });

  // When using the Guestbook footer link, keep the actual form accessible.
  for (const link of document.querySelectorAll('a[href="#comments"]')) {
    link.addEventListener("click", () => { disclosure.open = true; });
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!form.reportValidity()) return;
    const submit = form.querySelector("button[type='submit']");
    const original = submit.textContent;
    submit.disabled = true;
    submit.textContent = "Posting…";
    setStatus(status, "");
    try {
      const payload = Object.fromEntries(new FormData(form).entries());
      payload.name = String(payload.name || "").trim();
      payload.message = String(payload.message || "").trim();
      const { response, data } = await requestJson("/api/comments", {
        method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload)
      });
      setStatus(status, data.message || (response.ok ? "Comment posted." : "Could not post your comment."), response.ok ? "ok" : "error");
      if (response.ok) { form.reset(); await loadComments(); }
    } catch (_) {
      setStatus(status, "Connection problem. Please try again.", "error");
    } finally {
      submit.disabled = false;
      submit.textContent = original;
    }
  });
}

function initContact() {
  const form = document.getElementById("contact-form");
  const status = document.getElementById("contact-status");
  if (!form || !status) return;
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!form.reportValidity()) return;
    const submit = form.querySelector("button[type='submit']");
    const original = submit.textContent;
    submit.disabled = true;
    submit.textContent = "Sending…";
    setStatus(status, "");
    try {
      const { response, data } = await requestJson(form.action, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams(new FormData(form)).toString()
      });
      setStatus(status, data.message || (response.ok ? "Message received." : "Could not send your message."), response.ok ? "ok" : "error");
      if (response.ok) form.reset();
    } catch (_) {
      setStatus(status, "Connection problem. You can email me directly instead.", "error");
    } finally {
      submit.disabled = false;
      submit.textContent = original;
    }
  });
}
