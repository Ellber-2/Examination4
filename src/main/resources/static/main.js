document.getElementById('fetchScheduleBtn').addEventListener('click', function() {
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

    fetch('http://localhost:8080/api/fetch-schedule', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ timeeditLink })
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            // Extract and format the columns from each reservation, including starttime, startdate, endtime, and enddate
            const formattedData = data.reservations.map(reservation => {
                const columns = reservation.columns
                    .map(column => column.trim())
                    .filter(column => column !== '');

                const additionalValues = [
                    `Start Time: ${reservation.starttime}`,
                    `Start Date: ${reservation.startdate}`,
                    `End Time: ${reservation.endtime}`,
                    `End Date: ${reservation.enddate}`
                ];

                return [...columns, ...additionalValues].join('\n');
            }).filter(reservation => reservation !== '').join('\n\n');

            // Insert the formatted data into the existing textarea
            const jsonDataTextarea = document.getElementById('jsonData');
            jsonDataTextarea.value = formattedData;


        })
        .catch(error => {
            console.error('Error:', error);
            alert(`Error: ${error.message}`);
        });
});