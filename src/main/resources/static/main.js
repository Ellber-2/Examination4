document.getElementById('fetchScheduleBtn').addEventListener('click', function() {
    // Show the lightbox to input the URL
    const lightbox = document.getElementById('lightbox');
    const lightboxContent = document.querySelector('.lightbox-content');
    lightbox.style.display = 'block';
    document.body.style.overflow = 'hidden';
    setTimeout(() => {
        lightbox.classList.add('show');
        lightboxContent.classList.add('show');
    }, 10);
});

document.querySelectorAll('.close').forEach(closeBtn => {
    closeBtn.addEventListener('click', function() {
        const lightbox = this.closest('.lightbox');
        const lightboxContent = lightbox.querySelector('.lightbox-content');
        lightbox.classList.remove('show');
        lightboxContent.classList.remove('show');
        setTimeout(() => {
            lightbox.style.display = 'none';
            document.body.style.overflow = 'auto';
        }, 500);
    });
});

document.getElementById('scheduleForm').addEventListener('submit', function(event) {
    event.preventDefault();
    const timeeditLink = document.getElementById('TimeeditURL').value;

    if (!timeeditLink) {
        alert("Please enter a valid TimeEdit link.");
        return;
    }

    // Perform the fetch request to the backend API
    fetch('http://localhost:8080/api/fetch-schedule', {
        method: 'POST',  // Use POST method
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ timeeditLink })  // Send the link as a JSON object
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();  // Parse the JSON response
        })
        .then(data => {
            console.log(data);  // Log response for debugging

            // Display the JSON response in the textarea
            const jsonOutput = document.getElementById('jsonData');
            jsonOutput.textContent = JSON.stringify(data, null, 2);  // Format JSON for readability
        })
        .catch(error => {
            console.error('Error:', error);
            alert(`Error: ${error.message}`);
        });
});