import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class GestorAcademicoView {
    private static final Color FONDO = EstiloUVG.FONDO, AZUL = EstiloUVG.TEXTO,
            VERDE = EstiloUVG.VERDE, BORDE = EstiloUVG.BORDE, GRIS = new Color(111,121,114);
    private final JFrame ventana;
    private final JRootPane pantalla;
    private final GestorAcademicoController controlador;
    private final JPanel raiz = new JPanel(new BorderLayout(24,20));
    private final JComboBox<String> estado = new JComboBox<>(new String[]{"Todas","Pendientes","Completadas"});
    private final JComboBox<String> tipo = new JComboBox<>(new String[]{"Todos","Tarea","Proyecto","Examen"});
    private final JComboBox<String> orden = new JComboBox<>(new String[]{"Registro","Fecha","Prioridad"});
    private final JTextField busqueda = new JTextField(18);
    private final JLabel contador = new JLabel(), resumen = new JLabel(), resumenHoy = new JLabel();
    private final JLabel fechaElegida = new JLabel(), tituloMes = new JLabel("",SwingConstants.CENTER);
    private final JProgressBar progreso = new JProgressBar(0,100);
    private final JPanel calendario = new JPanel(new GridLayout(0,7,3,3));
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Tipo","Actividad","Entrega","Prioridad","Estado","Avisos"},0) {
        @Override public boolean isCellEditable(int fila,int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo) {
        private static final long serialVersionUID = 1L;
        @Override protected void paintComponent(Graphics graficos) {
            super.paintComponent(graficos);
            if (getRowCount() == 0) {
                Graphics2D g = (Graphics2D)graficos.create();
                g.setColor(GRIS); g.setFont(new Font("SansSerif",Font.PLAIN,14));
                String texto = "No hay actividades para mostrar";
                int x = Math.max(12,(getWidth()-g.getFontMetrics().stringWidth(texto))/2);
                g.drawString(texto,x,Math.max(70,getHeight()/2)); g.dispose();
            }
        }
    };
    private List<Actividad> visibles = new ArrayList<>();
    private YearMonth mes = YearMonth.now();
    private LocalDate filtroDia;
    private final Timer temporizador;
    private boolean avisoAbierto;

    public GestorAcademicoView(GestorAcademicoController controlador) {
        this.controlador = controlador;
        ventana = GraphicsEnvironment.isHeadless() ? null : new JFrame("Gestor Académico | UVG");
        pantalla = ventana == null ? new JRootPane() : ventana.getRootPane();
        if (ventana != null) {
            ventana.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            ventana.setSize(1200,780); ventana.setMinimumSize(new Dimension(1040,690));
            ventana.setLocationRelativeTo(null);
        }
        raiz.setBackground(FONDO); raiz.setBorder(new EmptyBorder(20,28,18,28)); pantalla.setContentPane(raiz);
        temporizador = new Timer(60_000, new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) {
                if (controlador.getUsuarioActual()!=null) {
                    try { refrescar(); revisarAvisos(); }
                    catch (IllegalStateException error) { mensaje(error.getMessage()); }
                }
            }
        });
        configurarTabla();
        ActionListener actualizarFiltros = new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) { refrescar(); }
        };
        estado.addActionListener(actualizarFiltros);
        tipo.addActionListener(actualizarFiltros);
        orden.addActionListener(actualizarFiltros);
        busqueda.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refrescar(); }
            public void removeUpdate(DocumentEvent e) { refrescar(); }
            public void changedUpdate(DocumentEvent e) { refrescar(); }
        });
        if (ventana != null) ventana.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (JOptionPane.showConfirmDialog(ventana,
                        "¿Cerrar la agenda?","Salir",
                        JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) { temporizador.stop(); ventana.dispose(); }
            }
        });
        mostrarLogin();
    }

    public void setVisible(boolean visible) { if (ventana != null) ventana.setVisible(visible); }
    private JRootPane getRootPane() { return pantalla; }
    JRootPane getContenido() { return pantalla; }
    void detenerTemporizador() { temporizador.stop(); }

    private JPanel vertical() {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); p.setOpaque(false); return p;
    }
    private JLabel titulo(String texto,int tamano) {
        JLabel l = new JLabel(texto); l.setFont(new Font("SansSerif",Font.BOLD,tamano)); l.setForeground(AZUL); return l;
    }
    private JButton boton(String texto,boolean principal,ActionListener accion) {
        JButton b = new EstiloUVG.Boton(texto, principal);
        b.addActionListener(accion); return b;
    }
    private JPanel tarjeta() {
        JPanel p = vertical(); p.setOpaque(true); p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(new LineBorder(BORDE),new EmptyBorder(20,20,20,20)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT); return p;
    }

    private JPanel marca() {
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        JPanel nombre = vertical();
        nombre.add(titulo("Gestor Académico", 20));
        nombre.add(Box.createVerticalStrut(5));
        JLabel universidad = new JLabel("Universidad del Valle de Guatemala");
        universidad.setForeground(GRIS); universidad.setFont(new Font("SansSerif",Font.PLAIN,12));
        nombre.add(universidad); cabecera.add(nombre, BorderLayout.CENTER);
        cabecera.add(EstiloUVG.logo(60), BorderLayout.EAST);
        return cabecera;
    }

    private void mostrarLogin() {
        raiz.removeAll(); JPanel centro = new JPanel(new GridBagLayout()); centro.setOpaque(false);
        raiz.add(marca(),BorderLayout.NORTH);
        JPanel tarjeta = tarjeta();
        tarjeta.setBorder(new CompoundBorder(new LineBorder(BORDE),new EmptyBorder(32,34,28,34)));
        tarjeta.add(titulo("Iniciar sesión",24)); tarjeta.add(Box.createVerticalStrut(28));
        JTextField usuario = new JTextField(26); JPasswordField clave = new JPasswordField(26);
        usuario.setPreferredSize(new Dimension(310,38)); clave.setPreferredSize(new Dimension(310,38));
        usuario.setMaximumSize(new Dimension(310,38)); clave.setMaximumSize(new Dimension(310,38));
        usuario.setBorder(new CompoundBorder(new LineBorder(BORDE),new EmptyBorder(6,10,6,10)));
        clave.setBorder(new CompoundBorder(new LineBorder(BORDE),new EmptyBorder(6,10,6,10)));
        tarjeta.add(new JLabel("Usuario")); tarjeta.add(Box.createVerticalStrut(7)); tarjeta.add(usuario);
        tarjeta.add(Box.createVerticalStrut(18)); tarjeta.add(new JLabel("Contraseña"));
        tarjeta.add(Box.createVerticalStrut(7)); tarjeta.add(clave); tarjeta.add(Box.createVerticalStrut(24));
        JLabel mensaje = new JLabel(" "); mensaje.setForeground(new Color(163,53,53));
        JButton entrar = boton("Entrar",true,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) {
            char[] secreto = clave.getPassword();
            try {
                if (controlador.iniciarSesion(usuario.getText(),new String(secreto))) { mostrarPrincipal(); }
                else {
                    mensaje.setText(controlador.isAccesoBloqueado() ? "Acceso bloqueado. Reinicia el programa."
                            : "Datos incorrectos. Quedan "+controlador.getIntentosRestantes()+" intentos.");
                    if (controlador.isAccesoBloqueado()) {
                        usuario.setEnabled(false); clave.setEnabled(false);
                        getRootPane().getDefaultButton().setEnabled(false); getRootPane().setDefaultButton(null);
                    }
                }
            } finally { Arrays.fill(secreto,'\0'); clave.setText(""); }
            }
        });
        entrar.setMaximumSize(new Dimension(310,40));
        tarjeta.add(entrar); tarjeta.add(Box.createVerticalStrut(16)); tarjeta.add(mensaje);
        for(Component c: tarjeta.getComponents()) {
            if(c instanceof JComponent) ((JComponent)c).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        centro.add(tarjeta); raiz.add(centro,BorderLayout.CENTER); getRootPane().setDefaultButton(entrar);
        raiz.revalidate(); raiz.repaint();
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { usuario.requestFocusInWindow(); }
        });
    }

    private void mostrarPrincipal() {
        raiz.removeAll(); getRootPane().setDefaultButton(null); filtroDia=null; mes=YearMonth.now();
        estado.setSelectedIndex(0); tipo.setSelectedIndex(0); orden.setSelectedIndex(0); busqueda.setText("");
        JPanel cabecera = marca();
        JPanel derecha = new JPanel(new BorderLayout(20,0)); derecha.setOpaque(false);
        JPanel sesion=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,10)); sesion.setOpaque(false);
        JLabel saludo = new JLabel(controlador.getUsuarioActual().getNombreUsuario());
        saludo.setForeground(GRIS); sesion.add(saludo);
        sesion.add(boton("Recordatorios",false,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) { mostrarRecordatoriosActivos(); }
        }));
        sesion.add(boton("Cerrar sesión",false,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) {
                temporizador.stop(); controlador.cerrarSesion(); mostrarLogin();
            }
        }));
        derecha.add(sesion,BorderLayout.CENTER); derecha.add(EstiloUVG.logo(60),BorderLayout.EAST);
        cabecera.add(derecha,BorderLayout.EAST); raiz.add(cabecera,BorderLayout.NORTH);
        JPanel cuerpo=new JPanel(new BorderLayout(22,0)); cuerpo.setOpaque(false);
        JPanel principal=new JPanel(new BorderLayout(0,16)); principal.setOpaque(false);
        JPanel controles=vertical(), encabezado=new JPanel(new BorderLayout()); encabezado.setOpaque(false);
        JPanel nombreSeccion=vertical(); nombreSeccion.add(titulo("Actividades",25));
        nombreSeccion.add(Box.createVerticalStrut(5));
        JLabel hoy = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM",new Locale("es","GT"))));
        hoy.setForeground(GRIS); hoy.setFont(new Font("SansSerif",Font.PLAIN,12)); nombreSeccion.add(hoy);
        encabezado.add(nombreSeccion);
        encabezado.add(boton("+ Nueva actividad",true,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) { mostrarFormulario(); }
        }),BorderLayout.EAST);
        controles.add(encabezado); controles.add(Box.createVerticalStrut(18));
        JPanel filtros=new JPanel(new FlowLayout(FlowLayout.LEFT,8,3)); filtros.setOpaque(false);
        filtros.add(new JLabel("Estado")); filtros.add(estado); filtros.add(new JLabel("Tipo")); filtros.add(tipo);
        filtros.add(new JLabel("Orden")); filtros.add(orden); controles.add(filtros);
        JPanel buscar=new JPanel(new FlowLayout(FlowLayout.LEFT,8,5)); buscar.setOpaque(false);
        buscar.add(new JLabel("Buscar")); buscar.add(busqueda);
        busqueda.setPreferredSize(new Dimension(245,32));
        busqueda.setBorder(new CompoundBorder(new LineBorder(BORDE),new EmptyBorder(6,9,6,9)));
        busqueda.setToolTipText("Nombre, descripción o materia");
        controles.add(buscar); principal.add(controles,BorderLayout.NORTH);
        JScrollPane scroll=new JScrollPane(tabla); scroll.setBorder(new LineBorder(BORDE));
        scroll.setColumnHeaderView(tabla.getTableHeader());
        scroll.getViewport().setBackground(Color.WHITE); principal.add(scroll);
        JPanel inferior=vertical(), acciones=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); acciones.setOpaque(false);
        acciones.add(boton("Completar / reabrir",false,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) { modificarSeleccion(true); }
        }));
        JButton aviso = boton("Recordatorio",false,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) { modificarSeleccion(false); }
        });
        aviso.setToolTipText("Activa o desactiva el aviso de la actividad seleccionada"); acciones.add(aviso);
        acciones.add(boton("Editar",false,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) {
                Actividad seleccion = seleccionada();
                if (seleccion != null) { mostrarFormulario(seleccion); }
            }
        }));
        acciones.add(boton("Eliminar",false,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) {
                Actividad seleccion = seleccionada();
                if (seleccion == null) { return; }
                if (JOptionPane.showConfirmDialog(ventana,
                        "¿Eliminar la actividad «" + seleccion.getNombre() + "»?", "Eliminar actividad",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    try { controlador.eliminarActividad(seleccion); refrescar(); }
                    catch (IllegalStateException error) { mensaje(error.getMessage()); }
                }
            }
        }));
        inferior.add(acciones); inferior.add(Box.createVerticalStrut(12)); contador.setForeground(GRIS);
        inferior.add(contador); principal.add(inferior,BorderLayout.SOUTH); cuerpo.add(principal);
        JPanel lateral=vertical(); lateral.setPreferredSize(new Dimension(286,500));
        JPanel avance=tarjeta(); avance.add(titulo("Progreso",16)); avance.add(Box.createVerticalStrut(16)); avance.add(resumen);
        avance.add(Box.createVerticalStrut(12)); progreso.setStringPainted(true); progreso.setForeground(VERDE);
        progreso.setBackground(EstiloUVG.SUAVE); progreso.setBorderPainted(false); progreso.setMaximumSize(new Dimension(245,20));
        avance.add(progreso); avance.add(Box.createVerticalStrut(12)); resumenHoy.setForeground(GRIS); avance.add(resumenHoy);
        lateral.add(avance); lateral.add(Box.createVerticalStrut(18)); JPanel fechas=tarjeta();
        fechas.add(titulo("Calendario",16)); fechas.add(Box.createVerticalStrut(16));
        JPanel nav=new JPanel(new BorderLayout(5,0)); nav.setOpaque(false); nav.setMaximumSize(new Dimension(245,34));
        nav.add(boton("‹",false,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) { mes=mes.minusMonths(1); dibujarCalendario(); }
        }),BorderLayout.WEST);
        nav.add(tituloMes);
        nav.add(boton("›",false,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) { mes=mes.plusMonths(1); dibujarCalendario(); }
        }),BorderLayout.EAST);
        fechas.add(nav); fechas.add(Box.createVerticalStrut(10)); calendario.setOpaque(false);
        calendario.setMaximumSize(new Dimension(245,230)); calendario.setPreferredSize(new Dimension(245,215)); fechas.add(calendario);
        fechas.add(Box.createVerticalStrut(12)); fechas.add(fechaElegida); fechas.add(Box.createVerticalStrut(10));
        fechas.add(boton("Ver todas las fechas",false,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) { filtroDia=null; refrescar(); }
        }));
        fechas.add(Box.createVerticalStrut(14)); JLabel ayuda=new JLabel("• indica un día con actividades");
        ayuda.setForeground(GRIS); ayuda.setFont(new Font("SansSerif",Font.PLAIN,11));
        ayuda.setText("• Día con actividades"); fechas.add(ayuda);
        for (JPanel panel : new JPanel[]{avance,fechas}) {
            for (Component componente : panel.getComponents()) {
                if (componente instanceof JComponent) {
                    ((JComponent)componente).setAlignmentX(Component.LEFT_ALIGNMENT);
                }
            }
        }
        resumenHoy.setMaximumSize(new Dimension(Integer.MAX_VALUE,24));
        lateral.add(fechas); cuerpo.add(lateral,BorderLayout.EAST); raiz.add(cuerpo);
        JLabel pie=new JLabel("Guardado automático"); pie.setFont(new Font("SansSerif",Font.PLAIN,11));
        pie.setToolTipText("Los cambios se guardan en data/actividades.xml. Los avisos requieren la aplicación abierta.");
        pie.setForeground(VERDE); raiz.add(pie,BorderLayout.SOUTH); refrescar(); raiz.revalidate(); raiz.repaint();
        temporizador.start();
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                try { revisarAvisos(); }
                catch (IllegalStateException error) { mensaje(error.getMessage()); }
            }
        });
    }

    private void configurarTabla() {
        tabla.setRowHeight(46); tabla.setFont(new Font("SansSerif",Font.PLAIN,13));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); tabla.setShowVerticalLines(false);
        tabla.setGridColor(BORDE); tabla.setFillsViewportHeight(true);
        tabla.setSelectionBackground(EstiloUVG.SUAVE); tabla.setSelectionForeground(AZUL);
        tabla.getTableHeader().setReorderingAllowed(false); tabla.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,12));
        tabla.getTableHeader().setBackground(new Color(242,245,242)); tabla.getTableHeader().setForeground(GRIS);
        tabla.getTableHeader().setPreferredSize(new Dimension(0,38));
        tabla.getColumnModel().getColumn(1).setPreferredWidth(220);
        int[] minimos = {82,140,108,90,98,78};
        int[] anchos = {84,280,112,94,102,82};
        for(int i=0;i<minimos.length;i++) {
            tabla.getColumnModel().getColumn(i).setMinWidth(minimos[i]);
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tabla.setToolTipText("Doble clic para ver el detalle de una actividad");
        tabla.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override public Component getTableCellRendererComponent(JTable t,Object valor,boolean seleccion,
                    boolean foco,int fila,int columna) {
                super.getTableCellRendererComponent(t,valor,seleccion,foco,fila,columna);
                setBackground(new Color(242,245,242)); setForeground(GRIS);
                setFont(new Font("SansSerif",Font.BOLD,12)); setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,BORDE),new EmptyBorder(0,10,0,10)));
                return this;
            }
        });
        tabla.setDefaultRenderer(Object.class,new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override public Component getTableCellRendererComponent(JTable t,Object valor,boolean seleccionado,
                    boolean foco,int fila,int columna) {
                super.getTableCellRendererComponent(t,valor,seleccionado,foco,fila,columna); setBorder(new EmptyBorder(0,9,0,9));
                if (!seleccionado) {
                    setBackground(Color.WHITE); setForeground(AZUL);
                    if (fila<visibles.size()) {
                        Actividad a=visibles.get(fila); if(a.isCompletada()) setForeground(GRIS);
                        if(columna==3) setForeground("Alta".equals(a.getPrioridad()) ? new Color(163,65,52)
                                : "Media".equals(a.getPrioridad()) ? new Color(139,106,41) : VERDE);
                    }
                } return this;
            }
        });
        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if(e.getClickCount()==2 && tabla.getSelectedRow()>=0) {
                    Actividad a=visibles.get(tabla.getSelectedRow()); mensaje(a.getNombre()+"\n"+a.getTipo()+" · "
                            +a.getFechaEntrega()+"\nPrioridad: "+a.getPrioridad()+"\n\n"+a.getDetalle());
                }
            }
        });
    }

    private void refrescar() {
        if(controlador.getUsuarioActual()==null) return;
        Actividad seleccion=tabla.getSelectedRow()>=0 && tabla.getSelectedRow()<visibles.size()
                ? visibles.get(tabla.getSelectedRow()) : null;
        visibles=controlador.listar((String)estado.getSelectedItem(),(String)tipo.getSelectedItem(),
                busqueda.getText(),(String)orden.getSelectedItem(),filtroDia); modelo.setRowCount(0);
        for(Actividad a:visibles) modelo.addRow(new Object[]{a.getTipo(),a.getNombre(),a.getFechaEntrega(),a.getPrioridad(),
                a.isCompletada() ? "Completada" : "Pendiente",a.isRecordatorioActivo() ? "Activos" : "Inactivos"});
        if(seleccion!=null && visibles.contains(seleccion)) {
            int fila=visibles.indexOf(seleccion); tabla.setRowSelectionInterval(fila,fila);
        }
        int[] total=controlador.obtenerResumen(null), hoy=controlador.obtenerResumen(LocalDate.now());
        resumen.setText(total[0]+" de "+total[1]+" completadas"); progreso.setValue(total[2]); progreso.setString(total[2]+" %");
        resumenHoy.setText("Hoy: "+hoy[0]+" de "+hoy[1]+" · "+hoy[2]+" %");
        contador.setText(visibles.size()+ (visibles.size()==1 ? " actividad" : " actividades"));
        contador.setFont(new Font("SansSerif",Font.PLAIN,12));
        fechaElegida.setText(filtroDia==null ? "Todas las fechas" : "Fecha: "+filtroDia); dibujarCalendario();
    }

    private void dibujarCalendario() {
        calendario.removeAll(); tituloMes.setText(mes.getMonth().getDisplayName(TextStyle.FULL,new Locale("es","GT"))+" "+mes.getYear());
        for(String dia:new String[]{"L","M","M","J","V","S","D"}) {
            JLabel l=new JLabel(dia,SwingConstants.CENTER); l.setForeground(GRIS); calendario.add(l);
        }
        for(int i=1;i<mes.atDay(1).getDayOfWeek().getValue();i++) calendario.add(new JLabel(""));
        List<Actividad> todas=controlador.getActividades();
        for(int i=1;i<=mes.lengthOfMonth();i++) {
            LocalDate dia=mes.atDay(i);
            boolean tiene=false;
            for (Actividad actividad : todas) {
                if (actividad.getFechaEntrega().equals(dia)) { tiene=true; break; }
            }
            JButton b=new JButton(i+(tiene ? "•" : "")); b.setMargin(new Insets(3,0,3,0)); b.setFocusPainted(false);
            b.setOpaque(true); b.setContentAreaFilled(true); b.setBorderPainted(dia.equals(LocalDate.now()));
            b.setBorder(new LineBorder(VERDE)); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setFont(new Font("SansSerif",dia.equals(LocalDate.now()) ? Font.BOLD : Font.PLAIN,11));
            b.setBackground(dia.equals(filtroDia) ? VERDE : Color.WHITE); b.setForeground(dia.equals(filtroDia) ? Color.WHITE : AZUL);
            b.setToolTipText(dia.toString());
            b.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent evento) { filtroDia=dia; refrescar(); }
            });
            calendario.add(b);
        }
        while(calendario.getComponentCount()<49) calendario.add(new JLabel(""));
        calendario.revalidate(); calendario.repaint();
    }

    private void mostrarFormulario() {
        mostrarFormulario(null);
    }

    private void mostrarFormulario(final Actividad actual) {
        JDialog dialogo=new JDialog(ventana,actual == null ? "Nueva actividad" : "Editar actividad",true);
        JPanel panel=new JPanel(new GridBagLayout()); panel.setBackground(Color.WHITE); panel.setBorder(new EmptyBorder(24,24,24,24));
        JComboBox<String> clase=new JComboBox<>(new String[]{"Tarea","Proyecto","Examen"}); JTextField nombre=new JTextField(28);
        JTextField fecha=new JTextField(LocalDate.now().toString());
        JComboBox<String> prioridad=new JComboBox<>(new String[]{"Alta","Media","Baja"}); prioridad.setSelectedItem("Media");
        JTextArea detalle=new JTextArea(5,28); detalle.setLineWrap(true); detalle.setWrapStyleWord(true);
        JLabel etiqueta=new JLabel("Descripción (opcional)");
        clase.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) {
                if ("Examen".equals(clase.getSelectedItem())) { etiqueta.setText("Materia (obligatoria)"); }
                else { etiqueta.setText("Descripción (opcional)"); }
            }
        });
        JCheckBox avisos=new JCheckBox("Activar recordatorios cada 12 horas"); avisos.setOpaque(false);
        if (actual != null) {
            clase.setSelectedItem(actual.getTipo()); clase.setEnabled(false);
            nombre.setText(actual.getNombre()); fecha.setText(actual.getFechaEntrega().toString());
            prioridad.setSelectedItem(actual.getPrioridad()); detalle.setText(actual.getDetalle());
            avisos.setSelected(actual.isRecordatorioActivo()); avisos.setEnabled(!actual.isCompletada());
        }
        Object[][] campos={{new JLabel("Tipo"),clase},{new JLabel("Nombre"),nombre},{new JLabel("Fecha (AAAA-MM-DD)"),fecha},
                {new JLabel("Prioridad"),prioridad},{etiqueta,new JScrollPane(detalle)}};
        GridBagConstraints g=new GridBagConstraints(); g.insets=new Insets(7,0,7,14); g.fill=GridBagConstraints.HORIZONTAL;
        for(int i=0;i<campos.length;i++) {
            g.gridy=i; g.gridx=0; g.weightx=0; panel.add((Component)campos[i][0],g);
            g.gridx=1; g.weightx=1; panel.add((Component)campos[i][1],g);
        }
        g.gridy=5; g.gridx=1; panel.add(avisos,g); g.gridy=6;
        panel.add(new JLabel("Los avisos funcionan con la aplicación abierta."),g);
        JButton guardar=boton("Guardar actividad",true,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) {
            try {
                if (actual == null) {
                    controlador.registrarActividad((String)clase.getSelectedItem(),nombre.getText(),fecha.getText(),
                            (String)prioridad.getSelectedItem(),detalle.getText(),avisos.isSelected());
                } else {
                    controlador.editarActividad(actual,nombre.getText(),fecha.getText(),
                            (String)prioridad.getSelectedItem(),detalle.getText(),avisos.isSelected());
                }
                dialogo.dispose(); refrescar();
            } catch(IllegalArgumentException | IllegalStateException error) {
                JOptionPane.showMessageDialog(dialogo,error.getMessage(),"Revisa los datos",JOptionPane.WARNING_MESSAGE);
            }
            }
        });
        JPanel acciones=new JPanel(new FlowLayout(FlowLayout.RIGHT)); acciones.setOpaque(false);
        acciones.add(boton("Cancelar",false,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) { dialogo.dispose(); }
        }));
        acciones.add(guardar);
        g.gridy=7; g.gridx=0; g.gridwidth=2; panel.add(acciones,g); dialogo.setContentPane(panel);
        dialogo.getRootPane().setDefaultButton(guardar); dialogo.pack(); dialogo.setResizable(false);
        dialogo.setLocationRelativeTo(ventana); dialogo.setVisible(true);
    }

    private void modificarSeleccion(boolean completar) {
        int fila=tabla.getSelectedRow(); if(fila<0) { mensaje("Selecciona una actividad de la lista."); return; }
        Actividad actividad=visibles.get(fila);
        try {
            if(completar) {
                if(actividad.isCompletada()) controlador.reabrirActividad(actividad);
                else controlador.marcarComoCompletada(actividad);
            } else controlador.cambiarRecordatorio(actividad);
            refrescar();
        } catch(IllegalArgumentException | IllegalStateException error) { mensaje(error.getMessage()); }
    }
    private Actividad seleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= visibles.size()) { mensaje("Selecciona una actividad de la lista."); return null; }
        return visibles.get(fila);
    }
    private void mostrarRecordatoriosActivos() {
        StringBuilder texto=new StringBuilder("Recordatorios activos\n\n"); int cantidad=0;
        for(Actividad a:controlador.getActividades()) if(a.isRecordatorioActivo() && !a.isCompletada()) {
            cantidad++; texto.append(a.getTipo()).append(": ").append(a.getNombre()).append("\nEntrega: ")
                    .append(a.getFechaEntrega()).append("\nPróximo aviso: ").append(a.getSiguienteRecordatorio()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n");
        }
        if(cantidad==0) texto.append("No hay avisos activos. Actívalos al registrar o seleccionar una actividad.");
        mostrarTexto(texto.toString(),"Recordatorios");
    }
    private void revisarAvisos() {
        if(avisoAbierto || controlador.getUsuarioActual()==null) return;
        LocalDateTime ahora=LocalDateTime.now(); List<Actividad> pendientes=controlador.obtenerRecordatoriosPendientes(ahora);
        if(pendientes.isEmpty()) return;
        StringBuilder texto=new StringBuilder("Tienes actividades pendientes:\n\n");
        for(Actividad a:pendientes) {
            texto.append(a.getTipo()).append(": ").append(a.getNombre()).append("\nEntrega: ").append(a.getFechaEntrega())
                    .append("\nPrioridad: ").append(a.getPrioridad()).append("\n\n"); controlador.confirmarRecordatorio(a,ahora);
        }
        avisoAbierto=true;
        try { mostrarTexto(texto.toString(),"Recordatorios académicos"); } finally { avisoAbierto=false; }
    }
    private void mostrarTexto(String texto,String titulo) {
        JTextArea area=new JTextArea(texto,16,55); area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true);
        area.setCaretPosition(0); JOptionPane.showMessageDialog(ventana,new JScrollPane(area),titulo,JOptionPane.INFORMATION_MESSAGE);
    }
    private void mensaje(String texto) { JOptionPane.showMessageDialog(ventana,texto); }
}
