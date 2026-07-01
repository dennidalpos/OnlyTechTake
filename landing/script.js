/**
 * OnlyTechTake Landing Page Logic
 * Aligned with App features (v1.1.0)
 */

const translations = {
    it: {
        meta: {
            title: "OnlyTechTake - Gestione Professionale Riparazioni",
            description: "Gestione professionale di commesse, foto e report PDF per laboratori di riparazione. Tutto sul tuo dispositivo Android in totale sicurezza."
        },
        nav: {
            features: "Funzioni",
            versions: "Versioni",
            downloadPro: "Scarica PRO",
            theme: "Cambia Tema",
            menu: "Menu",
            close: "Chiudi"
        },
        sidebar: {
            home: "Home",
            features: "Funzioni",
            gallery: "Gallery",
            versions: "Versioni",
            buyPro: "Acquista PRO",
            privacy: "Privacy Policy"
        },
        hero: {
            title: 'OnlyTechTake: Ripara, traccia, aggiorna.<br><span class="gradient-text">Il tuo Banco Riparazioni.</span>',
            subtitle: "Gestione professionale delle commesse, diagnosi e clienti tutto sul tuo dispositivo Android in totale sicurezza. Senza abbonamenti, senza cloud.",
            ctaPro: "Inizia ora (PRO)",
            ctaDemo: "Prova la Demo"
        },
        privacySection: {
            title: "Dati protetti, privacy garantita",
            text: "L'app opera interamente sul tuo dispositivo. Foto, firme e dati dei clienti non vengono mai inviati a server esterni o salvati online. Utilizziamo tecnologie di protezione avanzate per garantire che le informazioni della tua attività rimangano private e sicure."
        },
        features: {
            title: "Workflow Professionale",
            subtitle: "Progettato per laboratori che necessitano di velocità, precisione e sicurezza.",
            workbench: {
                title: "Banco Riparazioni",
                text: "Tieni traccia di ogni fase del lavoro: foto, note e stato di avanzamento della commessa sempre a portata di mano."
            },
            media: {
                title: "Documentazione Fotografica",
                text: "Scatta foto e video per documentare lo stato estetico e tecnico di ogni dispositivo ricevuto, evitando contestazioni."
            },
            signature: {
                title: "Firma & Privacy",
                text: "Firma digitale direttamente sul touch screen per l'accettazione della privacy e delle condizioni di riparazione."
            },
            pdf: {
                title: "Rapportini PDF Professionali",
                text: "Genera in un istante schede tecniche e ricevute dettagliate in PDF da consegnare al cliente o archiviare."
            },
            whatsapp: {
                title: "Invio stato con WhatsApp",
                text: "Informa i tuoi clienti sullo stato della riparazione inviando messaggi pronti o il rapportino PDF con un semplice click."
            },
            backup: {
                title: "Sicurezza e Controllo",
                text: "Esporta i tuoi dati in archivi protetti o tabelle Excel per gestire la tua contabilità in totale autonomia."
            }
        },
        gallery: {
            title: "Esperienza Moderna",
            subtitle: "Un'interfaccia intuitiva e veloce, pensata per chi deve gestire riparazioni ogni giorno.",
            overlay: {
                dashboard: "Dashboard & Code Operative",
                intake: "Presa in Carico Guidata",
                media: "Foto e Firma Digitale",
                quotes: "Diagnosi e Preventivi",
                repairing: "Intervento Tecnico",
                whatsapp: "Invio stato con WhatsApp",
                timeline: "Cronologia Eventi & Timeline",
                customers: "Anagrafica Clienti",
                customerDetail: "Dettaglio Storico Cliente"
            },
            alt: {
                dashboard: "Banco Riparazioni",
                newOrder: "Nuova Commessa",
                media: "Media e Firme",
                diagnosis: "Diagnosi",
                repairing: "Lavorazione",
                pdf: "PDF e WhatsApp",
                timeline: "Timeline Eventi",
                customers: "Archivio Clienti",
                detail: "Dettaglio Screenshot",
                close: "Chiudi"
            }
        },
        pricing: {
            title: "Scegli la tua versione",
            subtitle: "Paghi una volta, possiedi i tuoi dati per sempre. Niente Cloud, niente canoni mensili.",
            demo: {
                price: "Gratis",
                feature1: '<i data-lucide="check"></i> 1 Commessa attiva',
                feature2: '<i data-lucide="check"></i> Diagnosi e Foto',
                feature3: '<i data-lucide="check"></i> Firma Digitale su Touch',
                feature4: '<i data-lucide="check"></i> Accesso Admin locale',
                feature5: '<i data-lucide="x"></i> PDF senza Watermark',
                feature6: '<i data-lucide="x"></i> Gestione Operatori',
                feature7: '<i data-lucide="x"></i> Export Excel (XLSX)',
                feature8: '<i data-lucide="x"></i> Backup protetto completo',
                cta: "Scarica Demo"
            },
            pro: {
                badge: "Consigliato",
                price: "Licenza Unica",
                feature1: '<i data-lucide="check"></i> Commesse Illimitate',
                feature2: '<i data-lucide="check"></i> PDF Personalizzati (Senza Watermark)',
                feature3: '<i data-lucide="check"></i> Gestione Multi-operatore',
                feature4: '<i data-lucide="check"></i> Export XLSX & Backup Sicuro',
                feature5: '<i data-lucide="check"></i> Protezione Dati Avanzata',
                feature6: '<i data-lucide="check"></i> Supporto prioritario',
                feature7: '<i data-lucide="check"></i> Aggiornamenti gratuiti',
                cta: "Acquista PRO"
            }
        },
        footer: {
            subtitle: "La soluzione professionale per la gestione delle riparazioni, progettata per la massima sicurezza dei tuoi dati.",
            resources: "Risorse",
            support: "Supporto Tecnico",
            github: "GitHub Repository",
            disclaimer: "Nessuna garanzia legale implicita sui moduli di firma. Consultare un legale per la conformità normativa locale."
        },
        privacyPage: {
            titleMeta: "Privacy Policy - OnlyTechTake",
            title: "Informativa sulla Privacy",
            version: "Versione Corrente: 1.1.0 (Conforme GDPR)",
            intro: "L'applicazione OnlyTechTake (di seguito \"l'App\") è progettata per operare interamente sul tuo dispositivo per massimizzare la tutela della privacy. La presente informativa, redatta ai sensi del <strong>Regolamento UE 2016/679 (GDPR)</strong>, descrive come vengono gestiti i dati, basandoci su un principio fondamentale: i tuoi dati appartengono a te e non lasciano mai il tuo dispositivo senza il tuo comando manuale.",
            section1: {
                title: "1. Trattamento dei Dati Locali",
                text: "L'App non dispone di un server centrale o di un database cloud. Tutte le informazioni inserite, inclusi:",
                list: [
                    "Anagrafiche clienti, contatti e note fiscali",
                    "Dettagli tecnici sulle riparazioni e cronologia interventi",
                    "Documentazione multimediale (Foto e Video HD)",
                    "Firme digitali di presa in carico e accettazione privacy"
                ],
                footer: "Vengono memorizzati esclusivamente in un database protetto all'interno dello storage privato del tuo dispositivo Android. Nessun dato viene indicizzato dal sistema operativo o reso accessibile ad altre app."
            },
            section2: {
                title: "2. Sicurezza e Protezione",
                text: "Per garantire la protezione contro l'accesso fisico non autorizzato, l'app implementa una protezione a livello di database. L'accesso ai dati è protetto da credenziali locali. È responsabilità dell'utente utilizzare sistemi di sblocco sicuri (PIN, Biometria) e non condividere il codice di recupero."
            },
            section3: {
                title: "3. Permessi e Finalità",
                text: "L'App richiede permessi minimi, utilizzati esclusivamente per le finalità operative del laboratorio:",
                list: [
                    "<strong>Fotocamera e Microfono:</strong> Acquisizione di prove visive e audio dello stato del dispositivo.",
                    "<strong>Storage:</strong> Salvataggio locale dei PDF e degli export richiesti dall'utente.",
                    "<strong>Internet:</strong> Nessun traffico dati principale. Utilizzato solo per la verifica periodica della licenza tramite Google Play."
                ]
            },
            section4: {
                title: "4. Condivisione e Trasferimento Dati",
                text: "L'app <strong>non trasmette automaticamente</strong> dati a terzi. La condivisione avviene solo su iniziativa manuale dell'utente professionale:",
                list: [
                    "Invio manuale di PDF tramite WhatsApp o Email.",
                    "Esportazione file per finalità contabili esterne.",
                    "Backup protetti per la migrazione dei dati tra i propri dispositivi."
                ]
            },
            section5: {
                title: "5. Diritti dell'Interessato e Titolare",
                text: "Ai sensi del GDPR, il <strong>Titolare del Trattamento</strong> dei dati dei clienti finali è l'utilizzatore professionale dell'App. I clienti finali possono esercitare i propri diritti (accesso, rettifica, cancellazione, portabilità) rivolgendosi direttamente al laboratorio. L'App fornisce strumenti integrati per la <strong>cancellazione totale</strong> dei dati o dei singoli record per facilitare la conformità normativa."
            }
        }
    },
    en: {
        meta: {
            title: "OnlyTechTake - Professional Repair Management",
            description: "Professional management of repair orders, photos, and PDF reports for repair workshops. Everything stays secure on your Android device."
        },
        nav: {
            features: "Features",
            versions: "Versions",
            downloadPro: "Download PRO",
            theme: "Change Theme",
            menu: "Menu",
            close: "Close"
        },
        sidebar: {
            home: "Home",
            features: "Features",
            gallery: "Gallery",
            versions: "Versions",
            buyPro: "Buy PRO",
            privacy: "Privacy Policy"
        },
        hero: {
            title: 'OnlyTechTake: Repair, track, update.<br><span class="gradient-text">Your service workbench.</span>',
            subtitle: "Professional management of repair orders, diagnostics, and customers right on your Android device. No subscriptions, no cloud.",
            ctaPro: "Get started now (PRO)",
            ctaDemo: "Try the Demo"
        },
        privacySection: {
            title: "Protected data, guaranteed privacy",
            text: "The app operates entirely on your device. Photos, signatures, and customer data are never sent to external servers or saved online. We use advanced protection technologies to ensure your business information remains private and secure."
        },
        features: {
            title: "Professional workflow",
            subtitle: "Designed for workshops that need speed, accuracy, and security.",
            workbench: {
                title: "Repair workbench",
                text: "Track every stage of the job: photos, notes, and repair order status always at your fingertips."
            },
            media: {
                title: "Photo documentation",
                text: "Take photos and videos to document the visual and technical condition of each device received, avoiding disputes."
            },
            signature: {
                title: "Signature & Privacy",
                text: "Digital signature directly on the touch screen for privacy acceptance and repair terms."
            },
            pdf: {
                title: "Professional PDF reports",
                text: "Instantly generate technical sheets and detailed PDF receipts to give to the customer or archive."
            },
            whatsapp: {
                title: "Send status updates via WhatsApp",
                text: "Keep your customers informed by sending ready-made messages or the PDF report with a simple click."
            },
            backup: {
                title: "Security and Control",
                text: "Export your data to protected archives or Excel tables to manage your accounting in total autonomy."
            }
        },
        gallery: {
            title: "Modern Experience",
            subtitle: "An intuitive and fast interface, designed for those who manage repairs every day.",
            overlay: {
                dashboard: "Dashboard & Work Queues",
                intake: "Guided Intake",
                media: "Photos and Digital Signature",
                quotes: "Diagnosis and Quotes",
                repairing: "Technical Intervention",
                whatsapp: "Send status via WhatsApp",
                timeline: "Event History & Timeline",
                customers: "Customer Records",
                customerDetail: "Customer History"
            },
            alt: {
                dashboard: "Repair Workbench",
                newOrder: "New Repair Order",
                media: "Media and Signatures",
                diagnosis: "Diagnosis",
                repairing: "In Progress",
                pdf: "PDF and WhatsApp",
                timeline: "Event Timeline",
                customers: "Customer Archive",
                detail: "Screenshot Detail",
                close: "Close"
            }
        },
        pricing: {
            title: "Choose your version",
            subtitle: "Pay once and keep your data forever. No cloud, no recurring monthly fees.",
            demo: {
                price: "Free",
                feature1: '<i data-lucide="check"></i> 1 active repair order',
                feature2: '<i data-lucide="check"></i> Diagnosis and photos',
                feature3: '<i data-lucide="check"></i> Touch digital signature',
                feature4: '<i data-lucide="check"></i> Local admin access',
                feature5: '<i data-lucide="x"></i> PDF without watermark',
                feature6: '<i data-lucide="x"></i> Operator management',
                feature7: '<i data-lucide="x"></i> Excel export (XLSX)',
                feature8: '<i data-lucide="x"></i> Full protected backup',
                cta: "Download Demo"
            },
            pro: {
                badge: "Recommended",
                price: "One-time license",
                feature1: '<i data-lucide="check"></i> Unlimited repair orders',
                feature2: '<i data-lucide="check"></i> Custom PDFs (No watermark)',
                feature3: '<i data-lucide="check"></i> Multi-operator management',
                feature4: '<i data-lucide="check"></i> XLSX export & Secure Backup',
                feature5: '<i data-lucide="check"></i> Advanced Data Protection',
                feature6: '<i data-lucide="check"></i> Priority support',
                feature7: '<i data-lucide="check"></i> Free updates',
                cta: "Buy PRO"
            }
        },
        footer: {
            subtitle: "The professional solution for repair management, designed for maximum security of your data.",
            resources: "Resources",
            support: "Technical Support",
            github: "GitHub Repository",
            disclaimer: "No implied legal warranty is provided for signature forms. Consult a legal professional for local regulatory compliance."
        },
        privacyPage: {
            titleMeta: "Privacy Policy - OnlyTechTake",
            title: "Privacy Policy",
            version: "Current Version: 1.1.0 (GDPR Compliant)",
            intro: "The OnlyTechTake application (hereinafter the \"App\") is designed to operate entirely on your device to maximize privacy protection. This policy, drafted in accordance with <strong>EU Regulation 2016/679 (GDPR)</strong>, describes how data is handled based on a core principle: your data belongs to you and never leaves your device without your manual command.",
            section1: {
                title: "1. Local Data Processing",
                text: "The App does not use a central server or cloud database. All entered information, including:",
                list: [
                    "Customer records, contacts, and tax notes",
                    "Technical repair details and intervention history",
                    "Multimedia documentation (Photos and HD Videos)",
                    "Digital signatures for intake and privacy acceptance"
                ],
                footer: "is stored exclusively in a protected database within your Android device's private storage. No data is indexed by the OS or accessible to other apps."
            },
            section2: {
                title: "2. Security and Protection",
                text: "To protect against unauthorized physical access, the app implements database-level protection. Access to data is protected by local credentials. It is the user's responsibility to use secure unlock systems (PIN, Biometrics) and not to share the recovery code."
            },
            section3: {
                title: "3. Permissions and Purposes",
                text: "The App requires minimal permissions, used exclusively for the workshop's operational purposes:",
                list: [
                    "<strong>Camera and Microphone:</strong> Acquiring visual and audio evidence of the device's condition.",
                    "<strong>Storage:</strong> Local saving of PDFs and exports requested by the user.",
                    "<strong>Internet:</strong> No main data traffic. Used only for periodic license verification via Google Play."
                ]
            },
            section4: {
                title: "4. Data Sharing and Transfer",
                text: "The app <strong>does not automatically transmit</strong> data to third parties. Sharing only occurs through the professional user's manual initiative:",
                list: [
                    "Manual PDF sending via WhatsApp or Email.",
                    "File export for external accounting purposes.",
                    "Protected backups for data migration between your own devices."
                ]
            },
            section5: {
                title: "5. Data Subject Rights and Controller",
                text: "Under the GDPR, the <strong>Data Controller</strong> for end-customer data is the professional user of the App. End customers can exercise their rights (access, rectification, erasure, portability) by contacting the workshop directly. The App provides integrated tools for <strong>total data deletion</strong> or individual record removal to facilitate regulatory compliance."
            }
        }
    }
};

document.addEventListener('DOMContentLoaded', () => {
    // 1. Language Management
    const getInitialLanguage = () => {
        // Always default to English on opening as per request
        return 'en';
    };

    let currentLightboxIndex = 0;

    let updateCarouselGlobal = null;

    const applyTranslations = (lang) => {
        const t = translations[lang];
        if (!t) return;

        document.documentElement.lang = lang;
        if (t.meta.title) document.title = t.meta.title;

        // Update meta description
        document.querySelectorAll('[data-i18n-meta-description]').forEach(el => {
            const text = t.meta.description;
            if (text) el.setAttribute('content', text);
        });

        // Helper to get nested properties from translations object
        const getNested = (obj, path) => {
            if (!path) return null;
            return path.split('.').reduce((prev, curr) => prev ? prev[curr] : null, obj);
        };

        // Update all data-i18n elements
        document.querySelectorAll('[data-i18n]').forEach(el => {
            const path = el.getAttribute('data-i18n');
            const text = getNested(t, path);
            if (text) el.textContent = text;
        });

        // Update all data-i18n-html elements
        document.querySelectorAll('[data-i18n-html]').forEach(el => {
            const path = el.getAttribute('data-i18n-html');
            const html = getNested(t, path);
            if (html) {
                el.innerHTML = html;
                // Re-initialize icons if needed
                if (typeof lucide !== 'undefined') {
                    lucide.createIcons({
                        attrs: { class: ['lucide-icon'] },
                        nameAttr: 'data-lucide',
                        node: el
                    });
                }
            }
        });

        // Update all data-i18n-title elements
        document.querySelectorAll('[data-i18n-title]').forEach(el => {
            const path = el.getAttribute('data-i18n-title');
            const title = getNested(t, path);
            if (title) el.title = title;
        });

        // Update all data-i18n-aria-label elements
        document.querySelectorAll('[data-i18n-aria-label]').forEach(el => {
            const path = el.getAttribute('data-i18n-aria-label');
            const label = getNested(t, path);
            if (label) el.setAttribute('aria-label', label);
        });

        // Update all data-i18n-alt elements
        document.querySelectorAll('[data-i18n-alt]').forEach(el => {
            const path = el.getAttribute('data-i18n-alt');
            const alt = getNested(t, path);
            if (alt) el.alt = alt;
        });

        // Update active class on language buttons
        document.querySelectorAll('[data-lang-switch]').forEach(btn => {
            if (btn.getAttribute('data-lang-switch') === lang) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });

        // Update lightbox caption if open (Logic removed to avoid ReferenceError)
        /*
        const lightbox = document.getElementById('lightbox');
        if (lightbox && lightbox.classList.contains('active')) {
            updateLightbox(currentLightboxIndex);
        }
        */

        // Re-create icons for the whole document
        if (typeof lucide !== 'undefined') {
            lucide.createIcons();
        }
    };

    const setLanguage = (lang) => {
        localStorage.setItem('language', lang);
        applyTranslations(lang);
        // Restart typing effect if carousel exists
        if (typeof updateCarouselGlobal === 'function') {
            updateCarouselGlobal();
        }
    };

    // Initialize language
    const currentLang = getInitialLanguage();
    applyTranslations(currentLang);

    // Language switch listeners
    document.querySelectorAll('[data-lang-switch]').forEach(btn => {
        btn.addEventListener('click', () => {
            setLanguage(btn.getAttribute('data-lang-switch'));
        });
    });

    // 2. Initialize Lucide Icons
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }

    // 3. Smooth Scrolling
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const targetId = this.getAttribute('href');
            if (targetId === '#') {
                window.scrollTo({ top: 0, behavior: 'smooth' });
                return;
            }

            const targetElement = document.querySelector(targetId);
            if (targetElement) {
                targetElement.scrollIntoView({ behavior: 'smooth' });
            }
        });
    });

    // 4. Theme Toggle
    const themeToggle = document.getElementById('theme-toggle');
    const html = document.documentElement;

    if (themeToggle) {
        themeToggle.addEventListener('click', () => {
            const currentTheme = html.getAttribute('data-theme');
            const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
            html.setAttribute('data-theme', newTheme);
            localStorage.setItem('theme', newTheme);
        });

        // Load saved theme
        const savedTheme = localStorage.getItem('theme');
        if (savedTheme) {
            html.setAttribute('data-theme', savedTheme);
        }
    }

    // 5. Sidebar / Navigation Drawer
    const sidebarToggle = document.getElementById('sidebar-toggle');
    const sidebarClose = document.getElementById('sidebar-close');
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebar-overlay');

    if (sidebarToggle && sidebar && overlay) {
        const toggleSidebar = () => {
            sidebar.classList.toggle('active');
            overlay.classList.toggle('active');
            document.body.style.overflow = sidebar.classList.contains('active') ? 'hidden' : 'auto';
        };

        sidebarToggle.addEventListener('click', toggleSidebar);
        if (sidebarClose) sidebarClose.addEventListener('click', toggleSidebar);
        overlay.addEventListener('click', toggleSidebar);

        // Close sidebar on link click
        document.querySelectorAll('.sidebar-link').forEach(link => {
            link.addEventListener('click', () => {
                if (sidebar.classList.contains('active')) toggleSidebar();
            });
        });
    }

    // 6. Dynamic Gallery (Ring Carousel) Implementation
    const carouselTrack = document.querySelector('.carousel-track');
    const carouselItems = Array.from(document.querySelectorAll('.carousel-item'));
    const nextBtn = document.querySelector('.next-btn');
    const prevBtn = document.querySelector('.prev-btn');
    const dotsContainer = document.querySelector('.carousel-dots');

    let currentIndex = 0;
    let startX, currentX, isDragging = false;
    let typingTimer = null;

    if (carouselTrack && carouselItems.length > 0) {
        // Create Dots
        carouselItems.forEach((_, i) => {
            const dot = document.createElement('div');
            dot.classList.add('dot');
            if (i === 0) dot.classList.add('active');
            dot.addEventListener('click', () => goToSlide(i));
            dotsContainer.appendChild(dot);
        });

        const dots = document.querySelectorAll('.dot');

        const typeWriter = (element, text, speed = 40) => {
            if (typingTimer) clearInterval(typingTimer);
            element.textContent = '';
            element.classList.add('typing-cursor');

            let i = 0;
            typingTimer = setInterval(() => {
                if (i < text.length) {
                    element.textContent += text.charAt(i);
                    i++;
                } else {
                    clearInterval(typingTimer);
                    element.classList.remove('typing-cursor');
                }
            }, speed);
        };

        const updateCarousel = () => {
            const total = carouselItems.length;
            if (total === 0) return;

            const viewportWidth = window.innerWidth;
            const isMobile = viewportWidth < 768;

            carouselItems.forEach((item, i) => {
                item.classList.remove('active', 'visible');

                // Calculate relative index for circular positioning
                let diff = i - currentIndex;
                if (diff > total / 2) diff -= total;
                if (diff < -total / 2) diff += total;

                // Ring positioning
                const absDiff = Math.abs(diff);
                const isActive = diff === 0;

                // Show items that are close to center
                if (absDiff <= 2) {
                    item.classList.add('visible');
                    if (isActive) item.classList.add('active');
                }

                // 3D Transform Logic - Adjusted for stability on web servers
                const translateUnit = isMobile ? 160 : 220;
                const translateX = diff * translateUnit;
                const translateZ = isActive ? 100 : -absDiff * 150;
                const rotateY = diff * -25;
                const scale = isActive ? 1.1 : 0.85;
                const opacity = isActive ? 1 : (absDiff === 1 ? 0.6 : (absDiff === 2 ? 0.2 : 0));

                const transformValue = `translateX(${translateX}px) translateZ(${translateZ}px) rotateY(${rotateY}deg) scale(${scale})`;
                item.style.transform = transformValue;
                item.style.webkitTransform = transformValue; // Support for older webkit browsers
                item.style.opacity = opacity;
                item.style.zIndex = 10 - absDiff;
            });

            // Start typing effect for active item
            const activeItem = carouselItems[currentIndex];
            const textEl = activeItem.querySelector('.gallery-text');
            const path = textEl.getAttribute('data-i18n');
            const lang = document.documentElement.lang || 'en';
            const text = translations[lang].gallery.overlay[path.split('.').pop()];

            typeWriter(textEl, text);

            // Update dots
            dots.forEach((dot, i) => {
                dot.classList.toggle('active', i === currentIndex);
            });
        };

        const goToSlide = (index) => {
            currentIndex = (index + carouselItems.length) % carouselItems.length;
            updateCarousel();
        };

        const nextSlide = () => goToSlide(currentIndex + 1);
        const prevSlide = () => goToSlide(currentIndex - 1);

        if (nextBtn) nextBtn.addEventListener('click', nextSlide);
        if (prevBtn) prevBtn.addEventListener('click', prevSlide);

        // Improved Drag / Swipe Support
        const handleDragStart = (e) => {
            isDragging = true;
            startX = e.type === 'touchstart' ? e.touches[0].clientX : e.clientX;
            currentX = startX;
            carouselTrack.style.cursor = 'grabbing';
            // Disable transitions during drag for immediate feedback
            carouselItems.forEach(item => item.style.transition = 'none');
        };

        const handleDragMove = (e) => {
            if (!isDragging) return;
            if (e.cancelable) e.preventDefault();

            currentX = e.type === 'touchmove' ? e.touches[0].clientX : e.clientX;
            const diffX = currentX - startX;

            const total = carouselItems.length;
            const translateUnit = window.innerWidth < 768 ? 160 : 220;

            carouselItems.forEach((item, i) => {
                let diff = i - currentIndex;
                if (diff > total / 2) diff -= total;
                if (diff < -total / 2) diff += total;

                const absDiff = Math.abs(diff);
                const isActive = diff === 0;

                const rotateNudge = diffX / 10;
                const translateX = diff * translateUnit + diffX;
                const rotateY = diff * -25 + rotateNudge;
                const translateZ = isActive ? 100 : -absDiff * 150;
                const scale = isActive ? 1.1 : 0.85;

                const transformValue = `translateX(${translateX}px) translateZ(${translateZ}px) rotateY(${rotateY}deg) scale(${scale})`;
                item.style.transform = transformValue;
                item.style.webkitTransform = transformValue;
            });
        };

        const handleDragEnd = (e) => {
            if (!isDragging) return;
            isDragging = false;
            carouselTrack.style.cursor = 'grab';

            // Re-enable transitions
            carouselItems.forEach(item => item.style.transition = '');

            const diff = currentX - startX;
            if (Math.abs(diff) > 80) {
                if (diff > 0) prevSlide();
                else nextSlide();
            } else {
                updateCarousel();
            }
        };

        // Attach events to the track and window for robustness
        carouselTrack.addEventListener('mousedown', handleDragStart);
        window.addEventListener('mousemove', handleDragMove);
        window.addEventListener('mouseup', handleDragEnd);

        carouselTrack.addEventListener('touchstart', handleDragStart, { passive: false });
        carouselTrack.addEventListener('touchmove', handleDragMove, { passive: false });
        carouselTrack.addEventListener('touchend', handleDragEnd);

        // Handle item click (if not active, go to it)
        carouselItems.forEach((item, i) => {
            item.addEventListener('click', (e) => {
                if (i !== currentIndex) {
                    e.preventDefault();
                    e.stopPropagation();
                    goToSlide(i);
                }
            });
        });

        // Click on the sides of the track to navigate (increases clickable area)
        carouselTrack.addEventListener('click', (e) => {
            // Ignore if we were dragging (threshold of 10px)
            if (Math.abs(currentX - startX) > 10) return;

            // If click was on an item, it's already handled by the item's listener
            if (e.target.closest('.carousel-item')) return;

            const rect = carouselTrack.getBoundingClientRect();
            const clickX = e.clientX - rect.left;

            if (clickX < rect.width * 0.35) {
                prevSlide();
            } else if (clickX > rect.width * 0.65) {
                nextSlide();
            }
        });

        updateCarouselGlobal = updateCarousel;

        // Initial setup
        window.addEventListener('resize', updateCarousel);
        // Delay initial update to ensure translations are applied
        setTimeout(updateCarousel, 100);
    }

    // 7. Accessibility & Cleanup
    // Logic for lightbox removed to avoid conflict with swipe/drag.
});
