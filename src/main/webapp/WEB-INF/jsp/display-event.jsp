<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8">
    <title>STEP Capstone</title>
    <link id="style" rel="stylesheet" href="style.css">
    <link rel="icon" href="images/step-favicon.svg" type="image/svg" sizes="16x16">
    <script src="https://www.gstatic.com/firebasejs/7.15.1/firebase-app.js"></script>
    <script src="https://www.gstatic.com/firebasejs/7.15.1/firebase-auth.js"></script>
    <script src="https://www.gstatic.com/firebasejs/7.15.1/firebase-analytics.js"></script>
    <script src="script.js"></script>
  </head>
  <body id="body" onload="loadActions(); displayIndividualEvent(${id}, ${saved});">
    <div class="header"></div>
    <div class="event-display-container">
      <div class="event-left-details">
        <div class="event-display-image"></div>
        <div class = "event-left-text">
          <div class="event-display-header">
            <input type="hidden" id="name" value='${name}'>
            <div class = "event-display-title">${name}</div>
            <div class="attendee-count-container">
              <span class="attendee-count">${attendees}</span> 
              already attending
            </div>
          </div>
          <input type="hidden" id="desc" value='${description}'>
          <div id="event-display-description" class="event-display-description">
            ${description}
          </div>
          <div class="tags-container">Tags:
            <input type="hidden" id="tags-value" value='${tags}'>
          </div>
        </div>
        <div class="footer">
          <p>
              <a href="https://www.flaticon.com/authors/freepik"
              title="Freepik"> Freepik</a> from
              <a href="https://www.flaticon.com/" title="Flaticon">
              www.flaticon.com</a>
          </p>
        </div>
      </div>
      <div class="expand-details" onclick="toggleDetails()">
        <img id="expand-arrow" src="images/arrow-up.svg" alt="Expand"/>
      </div>
      <div class="event-right-details">
        <div class="event-time-location">
          <input type="hidden" id="date" value='${date}'>
          <input type="hidden" id="start" value='${start}'>
          <input type="hidden" id="end-value" value='${end}'>
          <div class="date">
            <p>Date: ${date}</p>
          </div>
          <div class="time">
            <p>Start Time: ${start}</p>
          </div>
          <p>Location: ${address}</p>
        </div>
        <div class = "event-display-options">
          <a class="save-event">Save Event</a>
          <a href="index.html" class="go-back">Go Back</a>
            <br>
          <div class="share-wrapper">
            <h3>Share</h3>
            <div class="share-container">
              <input type="hidden" id="event-key" value='${key}'>
              <a id="twitter-link" target="_blank">
                  <img src="images/twitter.svg" alt="Twitter"/>
              </a>
              <a id="fb-link" target="_blank">
                  <img src="images/facebook.svg" alt="Facebook"/>
              </a>
              <a id="mail-link" target="_blank">
                  <img src="images/gmail.svg" alt="Email"/>
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
    <footer>
      <p>Made as part of Google's STEP Internship Program.</p>
      <h4>Copyright &copy; 2020 All Rights Reserved by 
         <a href="https://www.google.com/">Google</a>.
      </h4>
    </footer>
  </body>
</html>
