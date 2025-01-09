document.addEventListener('DOMContentLoaded', function() {
    const columnNamesMapping = {
        '0': 'Aktivitet',
        '1': 'Lokal',
        '2': 'Lärare',
        '3': 'Studentförening',
        '4': 'Kommentar',
        '5': 'Möteslänk',
        '6': 'Kurskod/Kursnamn',
        '7': 'Grupp/Grupper',
        '8': 'Campus',
        '9': 'Syfte',
        '10': 'Utrustning',
        '11': 'Text',
    };

    const timeeditLinkInput = document.getElementById('timeeditLinkInput'); // Save the TimeEdit link input element
    const savedTimeeditLink = localStorage.getItem('timeeditLink'); // Load the saved TimeEdit link from localStorage
    if (savedTimeeditLink) {
        timeeditLinkInput.value = savedTimeeditLink;
    }

    document.getElementById('fetchScheduleBtn').addEventListener('click', function() {
        const timeeditLink = timeeditLinkInput.value;
        const timeeditLinkPattern = /^https:\/\/cloud\.timeedit\.net\/ltu\/web\/schedule1\/[a-zA-Z0-9]+\.html$/;

        if (!timeeditLink || !timeeditLinkPattern.test(timeeditLink)) {
            alert("Please enter a valid TimeEdit link.");
            return;
        }

        // Save the TimeEdit link to localStorage
        localStorage.setItem('timeeditLink', timeeditLink);

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

                // Display the JSON response in the form
                const formContainer = document.getElementById('jsonData');
                formContainer.innerHTML = '';  // Clear previous content
                createFormFields(formContainer, data);
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

    function createFormFields(container, data, parentKey = '') {
        for (const key in data) {
            if (data.hasOwnProperty(key)) {
                if (key === 'columnheaders' || key === 'info') {
                    continue;  // Skip the "columnheaders" and "info" keys
                }
                const value = data[key];
                const inputKey = parentKey ? `${parentKey}.${key}` : key;
                let displayKey = key === 'reservations' ? 'Kurspass' : key;

                if (typeof value === 'object' && value !== null) {
                    // Skip the fieldset if the starttime is "08:00"
                    if (value.starttime === '08:00') {
                        continue;
                    }

                    // Change the title for numbered fieldsets and "columns"
                    if (!isNaN(displayKey)) {
                        const column0 = value.columns ? value.columns[0] : 'Unknown';
                        const startDate = value.startdate || 'Unknown';
                        const startTime = value.starttime || 'Unknown';
                        const endTime = value.endtime || 'Unknown';
                        displayKey = `${column0} - ${startDate} (${startTime} - ${endTime})`;
                    } else if (displayKey === 'columns') {
                        displayKey = 'Information om passet';
                    }

                    const fieldset = document.createElement('fieldset');
                    const legend = document.createElement('legend');
                    legend.textContent = displayKey;
                    legend.style.cursor = 'pointer';
                    legend.addEventListener('click', function() {
                        const content = this.nextElementSibling;
                        if (content.style.display === 'none') {
                            content.style.display = 'block';
                            fieldset.classList.add('expanded');
                        } else {
                            content.style.display = 'none';
                            fieldset.classList.remove('expanded');
                        }
                    });
                    fieldset.appendChild(legend);

                    const content = document.createElement('div');
                    content.style.display = displayKey === 'Kurspass' ? 'block' : 'none';
                    createFormFields(content, value, inputKey);
                    fieldset.appendChild(content);

                    container.appendChild(fieldset);
                } else {
                    // Apply column name mapping for columns inside "Information om passet"
                    if (parentKey.endsWith('columns') && columnNamesMapping.hasOwnProperty(key)) {
                        displayKey = columnNamesMapping[key];
                    }

                    const label = document.createElement('label');
                    label.textContent = displayKey;
                    const input = document.createElement('input');
                    input.type = 'text';
                    input.name = inputKey;
                    input.value = value;
                    if (parentKey && parentKey.includes('08:00')) {
                        input.classList.add('exclude');
                    }
                    container.appendChild(label);
                    container.appendChild(input);
                }
            }
        }
    }

    document.getElementById('saveBtn').addEventListener('click', function() {
        const formData = new FormData(document.getElementById('jsonDataForm'));
        const jsonData = {};
        formData.forEach((value, key) => {
            if (!document.querySelector(`[name="${key}"]`).classList.contains('exclude')) {
                const keys = key.split('.');
                keys.reduce((acc, k, i) => {
                    if (i === keys.length - 1) {
                        acc[k] = value;
                    } else {
                        acc[k] = acc[k] || {};
                    }
                    return acc[k];
                }, jsonData);
            }
        });

        console.log('Updated JSON Data:', jsonData);
        // You can now send this updated JSON data to your backend if needed
    });
});