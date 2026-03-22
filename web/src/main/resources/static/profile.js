(function () {
  var statusEl = document.getElementById("profileStatus");

  fetch("/api/me/profile", { credentials: "same-origin" })
    .then(function (res) {
      if (!res.ok) throw new Error("load");
      return res.json();
    })
    .then(function (data) {
      document.getElementById("firstName").value = data.firstName || "";
      document.getElementById("lastName").value = data.lastName || "";
      document.getElementById("shippingZip").value = data.shippingZip || "";
      if (statusEl) statusEl.textContent = "";
    })
    .catch(function () {
      if (statusEl) {
        statusEl.textContent = "Could not load profile.";
        statusEl.style.color = "#b00020";
      }
    });
})();
