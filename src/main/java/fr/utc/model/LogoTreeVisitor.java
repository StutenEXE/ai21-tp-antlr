package fr.utc.model;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import fr.utc.gui.Traceur;
import fr.utc.parsing.LogoParser.AvContext;
import fr.utc.parsing.LogoParser.FloatContext;
import fr.utc.parsing.LogoParser.*;
import javafx.beans.property.StringProperty;

public class LogoTreeVisitor extends LogoStoppableTreeVisitor {
	fr.utc.gui.Traceur traceur;
	Log log;

	public LogoTreeVisitor() {
		traceur = new Traceur();
		log = new Log();
	}

	public StringProperty logProperty() {
		return log;
	}

	public fr.utc.gui.Traceur getTraceur() {
		return traceur;
	}

	/*
	 * Map des attributs associés à chaque noeud de l'arbre
	 * key = node, value = valeur de l'expression du node
	 */
	ParseTreeProperty<Double> atts = new ParseTreeProperty<Double>();

	public void setValue(ParseTree node, double value) {
		atts.put(node, value);
	}

	public double getValue(ParseTree node) {
		Double value = atts.get(node);
		if (value == null) {
			throw new NullPointerException();
		}
		return value;
	}

	@Override
	public Integer visitBc(BcContext ctx){
		traceur.setCrayon(true);
		log.defaultLog(ctx);
		log.appendLog("Crayon baisser");
		return 0;
	}
	
	@Override
	public Integer visitFpos(FposContext ctx){
		Pair<Integer, List<Double>> bilan = evaluateList(ctx.expr());
		if(bilan.a == 0 && bilan.b.size()==2) {
			traceur.teleport(bilan.b.get(0), bilan.b.get(1));
			log.defaultLog(ctx);
			log.appendLog("Possition définit à : ", String.valueOf(bilan.b));
			
			return 0;
		}
		return 1;
	}
	
	@Override
	public Integer visitLc(LcContext ctx){
		traceur.setCrayon(false);
		log.defaultLog(ctx);
		log.appendLog("Crayon Lever");
		return 0;
	}

	@Override
	public Integer visitTd(TdContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.td(bilan.b);
			log.defaultLog(ctx);
			log.appendLog("Tourne de", String.valueOf(bilan.b), "a droite");
		}
		return bilan.a;
	}
	
	@Override
	public Integer visitRe(ReContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.avance(-bilan.b);
			log.defaultLog(ctx);
			log.appendLog("Recule de ", String.valueOf(bilan.b));
		}
		return bilan.a;
	}
	
	@Override
	public Integer visitTg(TgContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.td(-bilan.b);
			log.defaultLog(ctx);
			log.appendLog("Tourne de", String.valueOf(bilan.b), "a gauche");
		}
		return bilan.a;
	}
	
	@Override 
	public Integer visitFcc(FccContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.setColor(bilan.b.intValue());
			log.defaultLog(ctx);
			log.appendLog("Couleur changer pour la n°", String.valueOf(bilan.b));
		}
		return bilan.a;
	}
	
	@Override
	public Integer visitFcap(FcapContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			log.defaultLog(ctx);
			log.appendLog("Cap de la tortue fixé à : ", String.valueOf(bilan.b));
			traceur.fixeCap(bilan.b);
		}
		return bilan.a;
	}

	@Override
	public Integer visitAv(AvContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.avance(bilan.b);
			// Différents type de log possibles. Voir classe Log
			log.defaultLog(ctx);
			log.appendLog("Avance de", String.valueOf(bilan.b));
		}
		return bilan.a;
	}

	// Expressions

	@Override
	public Integer visitFloat(FloatContext ctx) {
		String floatText = ctx.FLOAT().getText();
		setValue(ctx, Double.valueOf(floatText));
		return 0;
	}

	/**
	 * Visite le noeud expression
	 * S'il n'y a pas d'erreur (la valeur de retour de la visite vaut 0)
	 * on récupère la valeur de l'expressions à partir de la map
	 * sinon
	 * on affecte une valeur quelconque
	 * On retourne une paire, (code de visite, valeur)
	 * 
	 * @param expr
	 * @return
	 */
	private Pair<Integer, Double> evaluate(ParseTree expr) {
		int b = visit(expr);
		Double val = b == 0 ? getValue(expr) : Double.POSITIVE_INFINITY;
		return new Pair<Integer, Double>(b, val);
	}
	
	private Pair<Integer, List<Double>> evaluateList(List<ExprContext> expr) {
		List<Double> values = new ArrayList<>();
		int code = 0;
		for(ExprContext e : expr) {
			int b = visit(e);
			Double val = b == 0 ? getValue(e) : Double.POSITIVE_INFINITY;
			code +=b;
			values.add(val);
		}
		return new Pair<>(code, values);
	}

}
