package emprestimojogos.infra;

import emprestimojogos.excecao.ImagemInvalidaException;
import emprestimojogos.excecao.SistemaException;
import emprestimojogos.classes.Imagem;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Processa imagens com a API ImageIO do Java: limita a maior dimensão a
 * {@value #DIMENSAO_MAXIMA}px e regrava em JPEG com qualidade 80%. O arquivo vai para o
 * Armazenamento de Imagens (um diretório); o banco guarda só o caminho.
 */
public class ProcessadorImagensImageIO implements ProcessadorImagens {
    public static final long TAMANHO_MAXIMO_BYTES = 10L * 1024 * 1024;
    public static final int DIMENSAO_MAXIMA = 1024;
    private static final float QUALIDADE_JPEG = 0.8f;
    private static final Set<String> EXTENSOES = Set.of("jpg", "jpeg", "png");

    private final Path diretorio;

    public ProcessadorImagensImageIO(Path diretorio) {
        this.diretorio = diretorio;
    }

    @Override
    public Imagem processar(String nomeArquivo, byte[] conteudo) {
        if (Texto.vazio(nomeArquivo)) {
            throw new ImagemInvalidaException("Nome do arquivo de imagem não informado.");
        }
        if (!EXTENSOES.contains(extensao(nomeArquivo))) {
            throw new ImagemInvalidaException("Formato de imagem não suportado. Use JPG ou PNG.");
        }
        if (conteudo == null || conteudo.length == 0) {
            throw new ImagemInvalidaException("Arquivo de imagem vazio.");
        }
        if (conteudo.length > TAMANHO_MAXIMO_BYTES) {
            throw new ImagemInvalidaException("Imagem grande demais: o limite é 10 MB.");
        }

        BufferedImage original;
        try {
            original = ImageIO.read(new ByteArrayInputStream(conteudo));
        } catch (IOException e) {
            original = null;
        }
        if (original == null) {
            throw new ImagemInvalidaException("O arquivo enviado não é uma imagem válida.");
        }

        BufferedImage redimensionada = redimensionar(original);
        byte[] compactada = comprimirJpeg(redimensionada);
        try {
            Files.createDirectories(diretorio);
            Path destino = diretorio.resolve(UUID.randomUUID() + ".jpg");
            Files.write(destino, compactada);
            return new Imagem(nomeArquivo, compactada.length, destino.toString(),
                    redimensionada.getWidth(), redimensionada.getHeight());
        } catch (IOException e) {
            throw new SistemaException("Falha ao gravar a imagem.", e);
        }
    }

    @Override
    public void excluir(Imagem imagem) {
        try {
            Files.deleteIfExists(Path.of(imagem.getCaminho()));
        } catch (IOException ignorada) {
            // remoção do arquivo é melhor esforço; o registro já foi removido
        }
    }

    private static String extensao(String nome) {
        int ponto = nome.lastIndexOf('.');
        return ponto < 0 ? "" : nome.substring(ponto + 1).toLowerCase(Locale.ROOT);
    }

    private static BufferedImage redimensionar(BufferedImage original) {
        int largura = original.getWidth();
        int altura = original.getHeight();
        double escala = Math.min(1.0, (double) DIMENSAO_MAXIMA / Math.max(largura, altura));
        int novaLargura = Math.max(1, (int) Math.round(largura * escala));
        int novaAltura = Math.max(1, (int) Math.round(altura * escala));

        BufferedImage destino = new BufferedImage(novaLargura, novaAltura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(Color.WHITE); // fundo branco para PNGs com transparência
        g.fillRect(0, 0, novaLargura, novaAltura);
        g.drawImage(original, 0, 0, novaLargura, novaAltura, null);
        g.dispose();
        return destino;
    }

    private static byte[] comprimirJpeg(BufferedImage imagem) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam parametros = writer.getDefaultWriteParam();
        parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        parametros.setCompressionQuality(QUALIDADE_JPEG);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(saida)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(imagem, null, null), parametros);
        } catch (IOException e) {
            throw new SistemaException("Falha ao compactar a imagem.", e);
        } finally {
            writer.dispose();
        }
        return saida.toByteArray();
    }
}
