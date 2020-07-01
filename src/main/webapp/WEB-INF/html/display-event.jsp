<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8">
    <title>STEP Capstone</title>
    <link id="style" rel="stylesheet" href="style.css">
    <link rel="icon" href="images/step-favicon.svg" type="image/svg" sizes="16x16">
    <script src="script.js"></script>
  </head>
  <body id="body" onload="loadActions();">
    <div class="header"></div>
    <div class="event-display-container">
      <div class="event-left-details">
        <div class="event-display-image"></div>
        <div class = "event-left-text">
          <div class="event-display-header">
            <div class = "event-display-title">${name}</div>
            <div class="attendee-count-container">
              <span class="attendee-count environment-text">12</span> 
              already attending
            </div>
          </div>
          <div id="event-display-description" class="event-display-description">
            ${description}
          </div>
          <div class="tags-container">Tags:
            <span class="tag environment">environment</span>
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
          <p>Date: ${date}</p>
          <p>Time: ${start}</p>
          <p>Location: ${street}, ${city}, ${state}</p>
        </div>
        <div class = "event-display-options">
          <a href="my-events.html" class="save-event">Save Event</a>
          <a href="index.html" class="go-back">Go Back</a>
            <br>
          <div class="share-wrapper">
            <h3>Share</h3>
            <div class="share-container">
              <!-- hrefs will contain the correct link when the page is generated w/JS -->
              <a href = "https://twitter.com/share?url=http://google.com/" target="_blank">
                  <img src="images/twitter.svg" alt="Twitter"/>
              </a>
              <a href='http://www.facebook.com/sharer.php?u=http://google.com/' 
                  target="_blank">
                  <img src="images/facebook.svg" alt="Facebook"/>
              </a>
              <a href="mailto:?subject=Unite by STEP Event&body=Check out this event!
                  %0D%0A%0D%0Ahttp://google.com/" target="_blank">
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
