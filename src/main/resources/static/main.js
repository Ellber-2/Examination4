document.getElementById('fetchScheduleBtn').addEventListener('click', function() {
    const timeeditLink = 'https://cloud.timeedit.net/ltu/web/schedule1/ri105866X35Z0XQ6Z76g4Y85y7096Y35407gQY7Q557556906YQ93oZ7xacQfbZj7WQc.html'; // Example link

    if (!timeeditLink) {
        alert("Please enter a valid TimeEdit link.");
        return;
    }

    // Show the lightbox to show loading or results
    const lightbox = document.getElementById('lightbox');
    const lightboxContent = document.querySelector('.lightbox-content');
    lightbox.style.display = 'block';
    document.body.style.overflow = 'hidden';
    setTimeout(() => {
        lightbox.classList.add('show');
        lightboxContent.classList.add('show');
    }, 10);

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

            // Display the JSON response in the pre tag
            const jsonOutput = document.getElementById('jsonData');
            jsonOutput.textContent = JSON.stringify(data, null, 2);  // Format JSON for readability
        })
        .catch(error => {
            console.error('Error:', error);
            alert(`Error: ${error.message}`);

            // Handle errors by showing an alert and closing the lightbox
            setTimeout(() => {
                lightbox.classList.remove('show');
                lightboxContent.classList.remove('show');
                setTimeout(() => {
                    lightbox.style.display = 'none';
                    document.body.style.overflow = 'auto';
                }, 500);
            }, 500);
        });
});