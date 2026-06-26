/**
 * OnlyTechTake Landing Page Logic
 * Aligned with App features (v1.0.0)
 */

const translations = {
    it: {
        meta: {
            title: "OnlyTechTake - Gestione Riparazioni Local-First",
            description: "Gestione locale di commesse, foto e report PDF per laboratori di riparazione. Tutto sul tuo dispositivo Android."
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
            title: 'Un\'app per ogni problema!<br><span class="gradient-text">Il tuo Banco Riparazioni.</span>',
            subtitle: "L'applicazione Local-First professionale per gestire commesse, diagnosi e clienti in totale sicurezza. Senza abbonamenti, senza cloud.",
            ctaPro: "Inizia ora (PRO)",
            ctaDemo: "Prova la Demo"
        },
        privacySection: {
            title: "Dati locali, sicurezza SQLCipher",
            text: "OTT opera in modalità <strong>Local-First</strong>. Foto, firme e dati sensibili non lasciano mai il dispositivo. Il database è protetto da cifratura hardware-backed per garantirti la massima riservatezza professionale."
        },
        features: {
            title: "Workflow Professionale",
            subtitle: "Progettato per laboratori che necessitano di velocità, precisione e privacy.",
            workbench: {
                title: "Banco Riparazioni",
                text: "Gestisci il flusso operativo: Aperta, In Lavorazione, Diagnosi, Pronta al Ritiro e Chiusa."
            },
            media: {
                title: "Documentazione Media",
                text: "Scatta foto con CameraX per documentare lo stato estetico e tecnico di ogni dispositivo."
            },
            signature: {
                title: "Firma & Privacy",
                text: "Firma digitale su schermo per accettazione privacy e condizioni di presa in carico personalizzabili."
            },
            pdf: {
                title: "Report PDF Professionali",
                text: "Genera istantaneamente schede tecniche e ricevute PDF con il logo del tuo laboratorio."
            },
            whatsapp: {
                title: "WhatsApp Manuale",
                text: "Invia stati di avanzamento e PDF ai clienti tramite template predefiniti in un solo tap."
            },
            backup: {
                title: "Backup ZIP Cifrato",
                text: "Esporta l'intero archivio o singole tabelle XLSX per backup esterni in totale sicurezza."
            }
        },
        gallery: {
            title: "Esperienza Material 3",
            subtitle: "Interfaccia moderna e coerente con le linee guida Android, ottimizzata per il lavoro al banco.",
            overlay: {
                dashboard: "Dashboard & Code Operative",
                intake: "Presa in Carico Guidata",
                media: "Foto e Firma Digitale",
                quotes: "Diagnosi e Preventivi",
                whatsapp: "Invio PDF su WhatsApp",
                timeline: "Cronologia Eventi & Timeline",
                customers: "Anagrafica Clienti Locale"
            },
            alt: {
                dashboard: "Banco Riparazioni",
                newOrder: "Nuova Commessa",
                media: "Media e Firme",
                diagnosis: "Diagnosi",
                pdf: "PDF e Social",
                timeline: "Timeline Eventi",
                customers: "Archivio Clienti",
                detail: "Dettaglio Screenshot",
                close: "Chiudi"
            }
        },
        pricing: {
            title: "Scegli la tua versione",
            subtitle: "Paghi una volta, possiedi i tuoi dati per sempre. Niente Cloud, niente canoni.",
            demo: {
                price: "Gratis",
                feature1: '<i data-lucide="check"></i> 1 Commessa attiva',
                feature2: '<i data-lucide="check"></i> Diagnosi e Foto',
                feature3: '<i data-lucide="check"></i> Firma Digitale su Touch',
                feature4: '<i data-lucide="check"></i> Accesso Admin locale',
                feature5: '<i data-lucide="x"></i> PDF senza Watermark',
                feature6: '<i data-lucide="x"></i> Gestione Operatori',
                feature7: '<i data-lucide="x"></i> Export Excel (XLSX)',
                feature8: '<i data-lucide="x"></i> Backup ZIP completo',
                cta: "Scarica Demo"
            },
            pro: {
                badge: "Consigliato",
                price: "Licenza Unica",
                feature1: '<i data-lucide="check"></i> Commesse Illimitate',
                feature2: '<i data-lucide="check"></i> PDF Personalizzati (No Watermark)',
                feature3: '<i data-lucide="check"></i> Gestione Multi-operatore',
                feature4: '<i data-lucide="check"></i> Export XLSX & Backup ZIP',
                feature5: '<i data-lucide="check"></i> Cifratura SQLCipher Pro',
                feature6: '<i data-lucide="check"></i> Supporto prioritario',
                feature7: '<i data-lucide="check"></i> Aggiornamenti gratuiti',
                cta: "Acquista PRO"
            }
        },
        footer: {
            subtitle: "La soluzione professionale Local-First per i laboratori tecnici indipendenti.",
            resources: "Risorse",
            support: "Supporto Tecnico",
            github: "GitHub Repository",
            disclaimer: "Nessuna garanzia legale implicita sui moduli di firma. Consultare un legale per la conformità normativa locale."
        },
        privacyPage: {
            titleMeta: "Privacy Policy - OnlyTechTake",
            title: "Informativa sulla Privacy",
            version: "Versione Corrente: 1.1.0 (Conforme GDPR)",
            intro: "L'applicazione OnlyTechTake (di seguito \"l'App\") è progettata con un approccio Local-First per massimizzare la tutela della privacy. La presente informativa, redatta ai sensi del <strong>Regolamento UE 2016/679 (GDPR)</strong>, descrive come vengono gestiti i dati, basandoci su un principio fondamentale: i tuoi dati appartengono a te e non lasciano mai il tuo dispositivo senza il tuo comando manuale.",
            section1: {
                title: "1. Trattamento dei Dati Locali",
                text: "L'App non dispone di un server centrale o di un database cloud. Tutte le informazioni inserite, inclusi:",
                list: [
                    "Anagrafiche clienti, contatti e note fiscali",
                    "Dettagli tecnici sulle riparazioni e cronologia interventi",
                    "Documentazione multimediale (Foto CameraX)",
                    "Firme digitali di presa in carico e accettazione privacy"
                ],
                footer: "Vengono memorizzati esclusivamente in un <strong>database cifrato (SQLCipher)</strong> all'interno dello storage privato del tuo dispositivo Android. Nessun dato viene indicizzato dal sistema operativo o reso accessibile ad altre app."
            },
            section2: {
                title: "2. Sicurezza e Cifratura",
                text: "Per garantire la protezione contro l'accesso fisico non autorizzato, OnlyTechTake implementa una cifratura a livello di database. L'accesso ai dati è protetto da credenziali locali. È responsabilità dell'utente utilizzare sistemi di sblocco sicuri (PIN, Biometria) e non condividere il 'Master Code' del Kit di recupero."
            },
            section3: {
                title: "3. Permessi e Finalità",
                text: "L'App richiede permessi minimi, utilizzati esclusivamente per le finalità operative del laboratorio:",
                list: [
                    "<strong>Fotocamera:</strong> Acquisizione di prove visive dello stato del dispositivo.",
                    "<strong>Storage:</strong> Salvataggio locale dei PDF e degli export XLSX richiesti dall'utente.",
                    "<strong>Internet:</strong> Nessun traffico dati core. Utilizzato solo dai Google Play Services per la verifica periodica della licenza (Google Play Billing)."
                ]
            },
            section4: {
                title: "4. Condivisione e Trasferimento Dati",
                text: "OnlyTechTake <strong>non trasmette automaticamente</strong> dati a terzi. La condivisione avviene solo su iniziativa manuale dell'utente professionale:",
                list: [
                    "Invio manuale di PDF tramite protocolli di sistema (WhatsApp, Email).",
                    "Esportazione XLSX per finalità contabili esterne.",
                    "Backup ZIP cifrati per la migrazione del database tra dispositivi dell'utente."
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
            title: "OnlyTechTake - Local-First Repair Management",
            description: "Local management of repair orders, photos, and PDF reports for repair workshops. Everything stays on your Android device."
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
            title: 'One app for every repair.<br><span class="gradient-text">Your repair workbench.</span>',
            subtitle: "The professional Local-First app for managing repair orders, diagnostics, and customers securely. No subscriptions, no cloud.",
            ctaPro: "Get started now (PRO)",
            ctaDemo: "Try the Demo"
        },
        privacySection: {
            title: "Local data, SQLCipher security",
            text: "OTT works in <strong>Local-First</strong> mode. Photos, signatures, and sensitive data never leave the device. The database is protected with hardware-backed encryption to provide strong professional confidentiality."
        },
        features: {
            title: "Professional workflow",
            subtitle: "Designed for workshops that need speed, accuracy, and privacy.",
            workbench: {
                title: "Repair workbench",
                text: "Manage the operational flow: Open, In Progress, Diagnosis, Ready for Pickup, and Closed."
            },
            media: {
                title: "Media documentation",
                text: "Take photos with CameraX to document the visual and technical condition of each device."
            },
            signature: {
                title: "Signature & Privacy",
                text: "On-screen digital signature for privacy acceptance and customizable service intake terms."
            },
            pdf: {
                title: "Professional PDF reports",
                text: "Instantly generate technical sheets and PDF receipts with your workshop logo."
            },
            whatsapp: {
                title: "Manual WhatsApp sharing",
                text: "Send progress updates and PDFs to customers through predefined templates in one tap."
            },
            backup: {
                title: "Encrypted ZIP backup",
                text: "Export the entire archive or individual XLSX tables for secure external backups."
            }
        },
        gallery: {
            title: "Material 3 experience",
            subtitle: "A modern interface aligned with Android guidelines, optimized for workbench operations.",
            overlay: {
                dashboard: "Dashboard & Work Queues",
                intake: "Guided Intake",
                media: "Photos and Digital Signature",
                quotes: "Diagnosis and Quotes",
                whatsapp: "PDF Sharing via WhatsApp",
                timeline: "Event History & Timeline",
                customers: "Local Customer Records"
            },
            alt: {
                dashboard: "Repair Workbench",
                newOrder: "New Repair Order",
                media: "Media and Signatures",
                diagnosis: "Diagnosis",
                pdf: "PDF and Sharing",
                timeline: "Event Timeline",
                customers: "Customer Archive",
                detail: "Screenshot Detail",
                close: "Close"
            }
        },
        pricing: {
            title: "Choose your version",
            subtitle: "Pay once and keep your data forever. No cloud, no recurring fees.",
            demo: {
                price: "Free",
                feature1: '<i data-lucide="check"></i> 1 active repair order',
                feature2: '<i data-lucide="check"></i> Diagnosis and photos',
                feature3: '<i data-lucide="check"></i> Touch digital signature',
                feature4: '<i data-lucide="check"></i> Local admin access',
                feature5: '<i data-lucide="x"></i> PDF without watermark',
                feature6: '<i data-lucide="x"></i> Operator management',
                feature7: '<i data-lucide="x"></i> Excel export (XLSX)',
                feature8: '<i data-lucide="x"></i> Full ZIP backup',
                cta: "Download Demo"
            },
            pro: {
                badge: "Recommended",
                price: "One-time license",
                feature1: '<i data-lucide="check"></i> Unlimited repair orders',
                feature2: '<i data-lucide="check"></i> Custom PDFs, no watermark',
                feature3: '<i data-lucide="check"></i> Multi-operator management',
                feature4: '<i data-lucide="check"></i> XLSX export & ZIP backup',
                feature5: '<i data-lucide="check"></i> SQLCipher Pro encryption',
                feature6: '<i data-lucide="check"></i> Priority support',
                feature7: '<i data-lucide="check"></i> Free updates',
                cta: "Buy PRO"
            }
        },
        footer: {
            subtitle: "The professional Local-First solution for independent repair workshops.",
            resources: "Resources",
            support: "Technical Support",
            github: "GitHub Repository",
            disclaimer: "No implied legal warranty is provided for signature forms. Consult a legal professional for local regulatory compliance."
        },
        privacyPage: {
            titleMeta: "Privacy Policy - OnlyTechTake",
            title: "Privacy Policy",
            version: "Current Version: 1.1.0 (GDPR Compliant)",
            intro: "The OnlyTechTake application (hereinafter the \"App\") is designed with a Local-First approach to maximize privacy protection. This policy, drafted in accordance with <strong>EU Regulation 2016/679 (GDPR)</strong>, describes how data is handled based on a core principle: your data belongs to you and never leaves your device without your manual command.",
            section1: {
                title: "1. Local Data Processing",
                text: "The App does not use a central server or cloud database. All entered information, including:",
                list: [
                    "Customer records, contacts, and tax notes",
                    "Technical repair details and intervention history",
                    "Multimedia documentation (CameraX photos)",
                    "Digital signatures for intake and privacy acceptance"
                ],
                footer: "is stored exclusively in an <strong>encrypted database (SQLCipher)</strong> within your Android device's private storage. No data is indexed by the OS or accessible to other apps."
            },
            section2: {
                title: "2. Security and Encryption",
                text: "To protect against unauthorized physical access, OnlyTechTake implements database-level encryption. Access to data is protected by local credentials. It is the user's responsibility to use secure unlock systems (PIN, Biometrics) and not to share the 'Master Code' from the Recovery Kit."
            },
            section3: {
                title: "3. Permissions and Purposes",
                text: "The App requires minimal permissions, used exclusively for the workshop's operational purposes:",
                list: [
                    "<strong>Camera:</strong> Acquiring visual evidence of the device's condition.",
                    "<strong>Storage:</strong> Local saving of PDFs and XLSX exports requested by the user.",
                    "<strong>Internet:</strong> No core data traffic. Used only by Google Play Services for periodic license verification (Google Play Billing)."
                ]
            },
            section4: {
                title: "4. Data Sharing and Transfer",
                text: "OnlyTechTake <strong>does not automatically transmit</strong> data to third parties. Sharing only occurs through the professional user's manual initiative:",
                list: [
                    "Manual PDF sending via system protocols (WhatsApp, Email).",
                    "XLSX export for external accounting purposes.",
                    "Encrypted ZIP backups for database migration between user devices."
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
        const savedLang = localStorage.getItem('language');
        if (savedLang) return savedLang;

        const browserLang = navigator.language || navigator.userLanguage;
        if (browserLang.startsWith('en')) return 'en';

        return 'it';
    };

    let currentLightboxItem = null;

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

        // Update lightbox caption if open
        if (currentLightboxItem && lightboxCaption) {
            const overlayEl = currentLightboxItem.querySelector('.gallery-overlay');
            if (overlayEl) {
                const path = overlayEl.getAttribute('data-i18n');
                const caption = getNested(t, path) || overlayEl.innerText;
                lightboxCaption.innerText = caption;
            }
        }

        // Re-create icons for the whole document
        if (typeof lucide !== 'undefined') {
            lucide.createIcons();
        }
    };

    const setLanguage = (lang) => {
        localStorage.setItem('language', lang);
        applyTranslations(lang);
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

    // 2. Initialize Lucide Icons (called again in applyTranslations)
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

    // 6. Lightbox / Gallery Preview
    const lightbox = document.getElementById('lightbox');
    const lightboxImg = document.getElementById('lightbox-img');
    const lightboxCaption = document.getElementById('lightbox-caption');
    const closeLightboxBtn = document.querySelector('.close-lightbox');

    const openLightbox = (element) => {
        if (!lightbox || !lightboxImg) return;
        currentLightboxItem = element;

        const clickedImg = element.querySelector('img');
        const overlayEl = element.querySelector('.gallery-overlay');
        let caption = '';

        if (overlayEl) {
            const path = overlayEl.getAttribute('data-i18n');
            const lang = document.documentElement.lang;
            const t = translations[lang];

            const getNested = (obj, path) => {
                if (!path) return null;
                return path.split('.').reduce((prev, curr) => prev ? prev[curr] : null, obj);
            };

            caption = getNested(t, path) || overlayEl.innerText;
        }

        lightboxImg.src = clickedImg.src;
        if (lightboxCaption) lightboxCaption.innerText = caption;

        lightbox.classList.add('active');
        document.body.style.overflow = 'hidden';
    };

    const closeLightbox = () => {
        if (!lightbox) return;
        currentLightboxItem = null;
        lightbox.classList.remove('active');
        document.body.style.overflow = 'auto';
    };

    // Attach listeners to gallery items
    document.querySelectorAll('.gallery-item').forEach(item => {
        item.addEventListener('click', () => openLightbox(item));
    });

    // Close lightbox on button click or clicking outside the image
    if (closeLightboxBtn) closeLightboxBtn.addEventListener('click', closeLightbox);
    if (lightbox) {
        lightbox.addEventListener('click', (e) => {
            if (e.target === lightbox || e.target === closeLightboxBtn) {
                closeLightbox();
            }
        });
    }
});
