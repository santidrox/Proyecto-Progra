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

public class GestorAcademicoView extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Color FONDO = new Color(242,246,250), AZUL = new Color(24,52,77),
            VERDE = new Color(0,112,95), BORDE = new Color(222,230,237), GRIS = new Color(97,114,128);
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
    private final JTable tabla = new JTable(modelo);
    private List<Actividad> visibles = new ArrayList<>();
    private YearMonth mes = YearMonth.now();
    private LocalDate filtroDia;
    private final Timer temporizador;
    private boolean avisoAbierto;

    public GestorAcademicoView(GestorAcademicoController controlador) {
        super("Gestor Académico | UVG"); this.controlador = controlador;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); setSize(1220,800);
        setMinimumSize(new Dimension(1050,680)); setLocationRelativeTo(null);
        raiz.setBackground(FONDO); raiz.setBorder(new EmptyBorder(24,28,20,28)); setContentPane(raiz);
        temporizador = new Timer(60_000, new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) {
                if (controlador.getUsuarioActual()!=null) { refrescar(); revisarAvisos(); }
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
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (JOptionPane.showConfirmDialog(GestorAcademicoView.this,
                        "Los datos de esta versión se pierden al cerrar. ¿Deseas salir?","Salir",
                        JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) { temporizador.stop(); dispose(); }
            }
        });
        mostrarLogin();
    }

    private JPanel vertical() {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); p.setOpaque(false); return p;
    }
    private JLabel titulo(String texto,int tamano) {
        JLabel l = new JLabel(texto); l.setFont(new Font("SansSerif",Font.BOLD,tamano)); l.setForeground(AZUL); return l;
    }
    private JButton boton(String texto,boolean principal,ActionListener accion) {
        JButton b = new JButton(texto); b.setFont(new Font("SansSerif",Font.BOLD,12)); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); b.setOpaque(true);
        b.setBackground(principal ? VERDE : Color.WHITE); b.setForeground(principal ? Color.WHITE : AZUL);
        b.setBorder(new CompoundBorder(new LineBorder(principal ? VERDE : BORDE),new EmptyBorder(9,13,9,13)));
        b.addActionListener(accion); return b;
    }
    private JPanel tarjeta() {
        JPanel p = vertical(); p.setOpaque(true); p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(new LineBorder(BORDE),new EmptyBorder(20,20,20,20))); return p;
    }

    private void mostrarLogin() {
        raiz.removeAll(); JPanel centro = new JPanel(new GridBagLayout()); centro.setOpaque(false);
        JPanel tarjeta = tarjeta();
        tarjeta.setBorder(new CompoundBorder(new LineBorder(BORDE),new EmptyBorder(38,42,38,42)));
        tarjeta.add(titulo("Gestor Académico",30)); tarjeta.add(Box.createVerticalStrut(10));
        tarjeta.add(new JLabel("Tus tareas, proyectos y exámenes en un solo lugar.")); tarjeta.add(Box.createVerticalStrut(30));
        JTextField usuario = new JTextField(26); JPasswordField clave = new JPasswordField(26);
        usuario.setMaximumSize(new Dimension(440,36)); clave.setMaximumSize(new Dimension(440,36));
        tarjeta.add(new JLabel("Usuario")); tarjeta.add(Box.createVerticalStrut(7)); tarjeta.add(usuario);
        tarjeta.add(Box.createVerticalStrut(18)); tarjeta.add(new JLabel("Contraseña"));
        tarjeta.add(Box.createVerticalStrut(7)); tarjeta.add(clave); tarjeta.add(Box.createVerticalStrut(24));
        JLabel mensaje = new JLabel("Dispones de 3 intentos para iniciar sesión."); mensaje.setForeground(GRIS);
        JButton entrar = boton("Iniciar sesión",true,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) {
            char[] secreto = clave.getPassword();
            try {
                if (controlador.iniciarSesion(usuario.getText(),new String(secreto))) { mostrarPrincipal(); }
                else {
                    mensaje.setText(controlador.isAccesoBloqueado() ? "Acceso bloqueado. Reinicia el programa."
                            : "Datos incorrectos. Intentos restantes: "+controlador.getIntentosRestantes());
                    if (controlador.isAccesoBloqueado()) {
                        usuario.setEnabled(false); clave.setEnabled(false);
                        getRootPane().getDefaultButton().setEnabled(false); getRootPane().setDefaultButton(null);
                    }
                }
            } finally { Arrays.fill(secreto,'\0'); clave.setText(""); }
            }
        });
        tarjeta.add(entrar); tarjeta.add(Box.createVerticalStrut(18)); tarjeta.add(mensaje);
        centro.add(tarjeta); raiz.add(centro); getRootPane().setDefaultButton(entrar);
        raiz.revalidate(); raiz.repaint();
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { usuario.requestFocusInWindow(); }
        });
    }

    private void mostrarPrincipal() {
        raiz.removeAll(); getRootPane().setDefaultButton(null); filtroDia=null; mes=YearMonth.now();
        estado.setSelectedIndex(0); tipo.setSelectedIndex(0); orden.setSelectedIndex(0); busqueda.setText("");
        JPanel cabecera = new JPanel(new BorderLayout()); cabecera.setOpaque(false);
        JPanel textos=vertical(); textos.add(titulo("Tu agenda académica",30)); textos.add(Box.createVerticalStrut(7));
        JLabel saludo=new JLabel("Hola, "+controlador.getUsuarioActual().getNombreUsuario()+"   ·   "+LocalDate.now()
                .format(DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy",new Locale("es","GT"))));
        saludo.setForeground(GRIS); textos.add(saludo); cabecera.add(textos);
        JPanel sesion=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,4)); sesion.setOpaque(false);
        sesion.add(boton("Recordatorios",false,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) { mostrarRecordatoriosActivos(); }
        }));
        sesion.add(boton("Cerrar sesión",false,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) {
                temporizador.stop(); controlador.cerrarSesion(); mostrarLogin();
            }
        }));
        cabecera.add(sesion,BorderLayout.EAST); raiz.add(cabecera,BorderLayout.NORTH);
        JPanel cuerpo=new JPanel(new BorderLayout(22,0)); cuerpo.setOpaque(false);
        JPanel principal=new JPanel(new BorderLayout(0,16)); principal.setOpaque(false);
        JPanel controles=vertical(), encabezado=new JPanel(new BorderLayout()); encabezado.setOpaque(false);
        encabezado.add(titulo("Actividades",20));
        encabezado.add(boton("+ Nueva actividad",true,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) { mostrarFormulario(); }
        }),BorderLayout.EAST);
        controles.add(encabezado); controles.add(Box.createVerticalStrut(14));
        JPanel filtros=new JPanel(new FlowLayout(FlowLayout.LEFT,8,3)); filtros.setOpaque(false);
        filtros.add(new JLabel("Estado")); filtros.add(estado); filtros.add(new JLabel("Tipo")); filtros.add(tipo);
        filtros.add(new JLabel("Orden")); filtros.add(orden); controles.add(filtros);
        JPanel buscar=new JPanel(new FlowLayout(FlowLayout.LEFT,8,5)); buscar.setOpaque(false);
        buscar.add(new JLabel("Buscar")); buscar.add(busqueda); controles.add(buscar); principal.add(controles,BorderLayout.NORTH);
        JScrollPane scroll=new JScrollPane(tabla); scroll.setBorder(new LineBorder(BORDE));
        scroll.getViewport().setBackground(Color.WHITE); principal.add(scroll);
        JPanel inferior=vertical(), acciones=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); acciones.setOpaque(false);
        acciones.add(boton("Marcar completada",false,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) { modificarSeleccion(true); }
        }));
        acciones.add(boton("Activar / desactivar aviso",false,new ActionListener() {
            @Override public void actionPerformed(ActionEvent evento) { modificarSeleccion(false); }
        }));
        inferior.add(acciones); inferior.add(Box.createVerticalStrut(12)); contador.setForeground(GRIS);
        inferior.add(contador); principal.add(inferior,BorderLayout.SOUTH); cuerpo.add(principal);
        JPanel lateral=vertical(); lateral.setPreferredSize(new Dimension(286,500));
        JPanel avance=tarjeta(); avance.add(titulo("Tu avance",19)); avance.add(Box.createVerticalStrut(16)); avance.add(resumen);
        avance.add(Box.createVerticalStrut(12)); progreso.setStringPainted(true); progreso.setForeground(VERDE);
        progreso.setBackground(FONDO); progreso.setBorderPainted(false); progreso.setMaximumSize(new Dimension(245,25));
        avance.add(progreso); avance.add(Box.createVerticalStrut(12)); resumenHoy.setForeground(GRIS); avance.add(resumenHoy);
        lateral.add(avance); lateral.add(Box.createVerticalStrut(18)); JPanel fechas=tarjeta();
        fechas.add(titulo("Calendario",19)); fechas.add(Box.createVerticalStrut(16));
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
        ayuda.setForeground(GRIS); fechas.add(ayuda); lateral.add(fechas); cuerpo.add(lateral,BorderLayout.EAST); raiz.add(cuerpo);
        JLabel pie=new JLabel("Datos temporales: se pierden al cerrar   ·   Avisos cada 12 h con la aplicación abierta");
        pie.setForeground(GRIS); raiz.add(pie,BorderLayout.SOUTH); refrescar(); raiz.revalidate(); raiz.repaint();
        temporizador.start();
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { revisarAvisos(); }
        });
    }

    private void configurarTabla() {
        tabla.setRowHeight(40); tabla.setFont(new Font("SansSerif",Font.PLAIN,13));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); tabla.setShowVerticalLines(false);
        tabla.setGridColor(BORDE); tabla.setFillsViewportHeight(true);
        tabla.setSelectionBackground(new Color(219,239,233)); tabla.setSelectionForeground(AZUL);
        tabla.getTableHeader().setReorderingAllowed(false); tabla.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,12));
        tabla.getTableHeader().setBackground(new Color(231,238,244)); tabla.getTableHeader().setPreferredSize(new Dimension(0,38));
        tabla.getColumnModel().getColumn(1).setPreferredWidth(220);
        tabla.setDefaultRenderer(Object.class,new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override public Component getTableCellRendererComponent(JTable t,Object valor,boolean seleccionado,
                    boolean foco,int fila,int columna) {
                super.getTableCellRendererComponent(t,valor,seleccionado,foco,fila,columna); setBorder(new EmptyBorder(0,9,0,9));
                if (!seleccionado) {
                    setBackground(fila%2==0 ? Color.WHITE : new Color(247,250,252)); setForeground(AZUL);
                    if (fila<visibles.size()) {
                        Actividad a=visibles.get(fila); if(a.isCompletada()) setForeground(GRIS);
                        if(columna==3) setForeground("Alta".equals(a.getPrioridad()) ? new Color(175,46,46)
                                : "Media".equals(a.getPrioridad()) ? new Color(145,94,12) : VERDE);
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
        resumen.setText(total[0]+" de "+total[1]+" completadas en total"); progreso.setValue(total[2]); progreso.setString(total[2]+" %");
        resumenHoy.setText("Hoy: "+hoy[0]+" / "+hoy[1]+" actividades · "+hoy[2]+" %");
        contador.setText(visibles.size()+" actividades visibles · Doble clic para ver el detalle");
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
        JDialog dialogo=new JDialog(this,"Nueva actividad",true);
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
                controlador.registrarActividad((String)clase.getSelectedItem(),nombre.getText(),fecha.getText(),
                        (String)prioridad.getSelectedItem(),detalle.getText(),avisos.isSelected()); dialogo.dispose(); refrescar();
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
        dialogo.setLocationRelativeTo(this); dialogo.setVisible(true);
    }

    private void modificarSeleccion(boolean completar) {
        int fila=tabla.getSelectedRow(); if(fila<0) { mensaje("Selecciona una actividad de la lista."); return; }
        Actividad actividad=visibles.get(fila);
        try {
            if(completar) controlador.marcarComoCompletada(actividad); else controlador.cambiarRecordatorio(actividad);
            refrescar();
        } catch(IllegalArgumentException | IllegalStateException error) { mensaje(error.getMessage()); }
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
        area.setCaretPosition(0); JOptionPane.showMessageDialog(this,new JScrollPane(area),titulo,JOptionPane.INFORMATION_MESSAGE);
    }
    private void mensaje(String texto) { JOptionPane.showMessageDialog(this,texto); }
}
