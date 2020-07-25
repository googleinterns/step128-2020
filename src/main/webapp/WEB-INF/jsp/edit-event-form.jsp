<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8">
    <title>Edit Event</title>
    <link id="style" rel="stylesheet" href="style.css">
    <link rel="icon" href="images/step-favicon.svg" type="image/svg" sizes="16x16">
    <script src="https://www.gstatic.com/firebasejs/7.15.1/firebase-app.js"></script>
    <script src="https://www.gstatic.com/firebasejs/7.15.1/firebase-auth.js"></script>
    <script src="https://www.gstatic.com/firebasejs/7.15.1/firebase-analytics.js"></script>
    <script src="script.js"></script>
  </head>
  <body onload="loadActions(confirmUser);">
    <div class="header"></div>
    <div class="form-container">
      <h1>Edit event</h1>
      <form action="/edit-event" method="POST" id="eventform" name="eventform">
        <input type="hidden" id="key" name="key" value="${key}">
        <div class="form-section">
          <div class="form-label">
            <label for="event-name">*Event name:</label>
          </div>
          <div class="form-input">
            <input type="text" id="event-name" name="event-name" required><br>
            <input type="hidden" id="name-value" value="${name}">
          </div>
        </div>
        <div class="form-section">
          <div class="form-label">
            <label for="event-description">*Description:</label>
          </div>
          <div class="form-input">
            <textarea id="event-description" name="event-description"
              style="height:100px" required></textarea><br>
            <input type="hidden" id="desc-value" value="${description}">
          </div>
        </div>
        <div class="form-section">
          <div class="form-label">
            <label for="street-address">*Street address:</label>
          </div>
          <div class="form-input">
            <input type="text" id="street-address" name="street-address" required><br>
            <input type="hidden" id="address-value" value="${address}">
          </div>
        </div>
        <div class="form-section">
          <div class="form-label">
            <label for="city">*City:</label>
          </div>
          <div class="form-input">
            <input type="text" id="city" name="city" required><br>
          </div>
        </div>
        <div class="form-section">
          <div class="form-label">
            <label for="sate">*State:</label>
          </div>
          <div class="form-input">
            <input type="text" id="state" name="state" required><br>
          </div>
        </div>
        <div class="form-section">
          <div class="form-label">
            <label for="date">*Date:</label>
          </div>
          <div class="form-input">
            <input type="date" id="date" name="date" required><br>
            <input type="hidden" id="date-value" value="${date}">
          </div>
        </div>
        <div class="form-section">
          <div class="form-label">
            <label for="start-time">*Start time:</label>
          </div>
          <div class="form-input">
            <input type="time" id="start-time" name="start-time" required><br>
            <input type="hidden" id="start-value" value="${start}">
          </div>
        </div>
        <div class="form-section">
          <div class="form-label">
            <label for="end-time">End Time:</label>
          </div>
          <div class="form-input">
            <input type="time" id="end-time" name="end-time"><br>
            <input type="hidden" id="end-value" value="${end}">
          </div>
        </div>
        <div class="form-section">
          <div class="form-label">
            <div id="tags-label">
              <label for="tags">*Tags:</label>
            </div>
          </div>
          <div class="form-input">
            <div class="tag-box"></div>
            <input type="hidden" id="tags-value" value='${tags}'>
          </div>
        </div>
        <div class="form-section">
          <div class="form-label">
            <label for="cover-photo">Cover photo:</label>
          </div>
          <div class="form-input">
            <input type="file" id="cover-photo" name="cover-photo"><br>
          </div>
        </div>
        <div class="form-buttons">
          <a href="javascript:verifyTags()">
            <img src="images/submit-button.svg" alt="Submit button.">
          </a>
          <div class="divider"></div>
          <a href="/my-events.html">
            <img src="images/cancel-button.svg" alt="Cancel button.">
          </a>
          <div class="edit-divider"></div>
          <a href="/my-events.html">
            <img src="images/delete-button.svg" alt="Delete button.">
          </a>
        </div>
      </form>
    </div>
    <footer>
      <p>
          Icons made by 
          <a href="https://www.flaticon.com/authors/pixel-perfect" target="_blank">
          Pixel Perfect</a> &#38;
          <a href="https://www.flaticon.com/authors/freepik" target="_blank">
          Freepik</a> from
          <a href="https://www.flaticon.com/" title="Flaticon" target="_blank">
          www.flaticon.com</a>
      </p>
      <p>Made as part of Google's STEP Internship Program.</p>
      <h4>Copyright &copy; 2020 All Rights Reserved by 
         <a href="https://www.google.com/">Google</a>.
      </h4>
    </footer>
  </body>
</html>
